package com.agentplatform.chat.orchestrator;

import com.agentplatform.chat.dto.SendMessageRequest;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.entity.ChatSessionStateEntity;
import com.agentplatform.chat.llm.LlmChunk;
import com.agentplatform.chat.llm.LlmMessage;
import com.agentplatform.chat.llm.LlmStreamService;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionStateMapper;
import com.agentplatform.chat.service.ChatSessionService;
import com.agentplatform.chat.sse.IdempotencyService;
import com.agentplatform.chat.sse.SseEventBuilder;
import com.agentplatform.chat.sse.SseEventBuilder.SseEventWithMeta;
import com.agentplatform.chat.sse.SseEventCacheService;
import com.agentplatform.chat.tool.ToolDispatcher;
import com.agentplatform.chat.tool.ToolResult;
import com.agentplatform.common.core.agent.AgentConfigProvider;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Core chat orchestration loop (§3.4).
 * Manages: context building → LLM streaming → tool dispatch → SSE event pushing.
 */
@Component
public class ChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final int DEFAULT_MAX_STEPS = 10;

    private static final String DEFAULT_MODEL_ID = "gpt-4o";

    private final ChatSessionService sessionService;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionStateMapper sessionStateMapper;
    private final LlmStreamService llmStreamService;
    private final ToolDispatcher toolDispatcher;
    private final ObjectMapper objectMapper;
    private final Optional<AgentConfigProvider> agentConfigProvider;
    private final Optional<IdempotencyService> idempotencyService;
    private final Optional<SseEventCacheService> sseEventCacheService;
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());

    public ChatOrchestrator(ChatSessionService sessionService,
                            ChatMessageMapper messageMapper,
                            ChatSessionStateMapper sessionStateMapper,
                            LlmStreamService llmStreamService,
                            ToolDispatcher toolDispatcher,
                            ObjectMapper objectMapper,
                            Optional<AgentConfigProvider> agentConfigProvider,
                            Optional<IdempotencyService> idempotencyService,
                            Optional<SseEventCacheService> sseEventCacheService) {
        this.sessionService = sessionService;
        this.messageMapper = messageMapper;
        this.sessionStateMapper = sessionStateMapper;
        this.llmStreamService = llmStreamService;
        this.toolDispatcher = toolDispatcher;
        this.objectMapper = objectMapper;
        this.agentConfigProvider = agentConfigProvider;
        this.idempotencyService = idempotencyService;
        this.sseEventCacheService = sseEventCacheService;
    }

    public SseEmitter handleSendMessage(UUID sessionId, SendMessageRequest request, UUID userId) {
        ChatSessionEntity session = sessionService.getSessionOrThrow(sessionId, userId);

        if (request.getIdempotencyKey() != null && idempotencyService.isPresent()) {
            String existingMsgId = idempotencyService.get().checkAndRegister(
                    sessionId, request.getIdempotencyKey(), toJson(request));
            if (existingMsgId != null) {
                return replayCachedEvents(existingMsgId);
            }
        }

        ChatMessageEntity userMessage = new ChatMessageEntity()
                .setId(UUID.randomUUID())
                .setSessionId(sessionId)
                .setRole("user")
                .setContent(request.getContent())
                .setStatus("complete")
                .setAgentId(session.getCurrentAgentId())
                .setCreatedAt(OffsetDateTime.now());
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            userMessage.setAttachments(toJson(request.getAttachments()));
        }
        messageMapper.insert(userMessage);

        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String messageId = UUID.randomUUID().toString();

        UUID agentId = session.getCurrentAgentId();
        int maxSteps = resolveMaxSteps(agentId);

        return startStreamReply(session, requestId, messageId, maxSteps, 0, false,
                request.getIdempotencyKey());
    }

    public SseEmitter handleRegenerate(UUID sessionId, UUID msgId, UUID userId) {
        ChatSessionEntity session = sessionService.getSessionOrThrow(sessionId, userId);

        ChatMessageEntity original = messageMapper.selectById(msgId);
        if (original == null || !original.getSessionId().equals(sessionId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }

        original.setContent(null);
        original.setToolCalls(null);
        original.setToolResults(null);
        original.setStatus("incomplete");
        original.setUsage(null);
        original.setStepCount(null);
        messageMapper.updateById(original);

        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String messageId = original.getId().toString();

        int maxSteps = resolveMaxSteps(session.getCurrentAgentId());

        return startStreamReply(session, requestId, messageId, maxSteps, 0, true, null);
    }

    public SseEmitter handleReconnect(UUID sessionId, String lastEventId, UUID userId) {
        sessionService.getSessionOrThrow(sessionId, userId);

        String messageId = sseEventCacheService.map(svc -> svc.resolveMessageId(sessionId.toString())).orElse(null);
        if (messageId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Thread.ofVirtual().name("sse-reconnect-" + sessionId).start(() -> {
            try {
                var events = sseEventCacheService.map(svc -> svc.getEventsAfter(messageId, lastEventId)).orElse(List.of());
                for (var cached : events) {
                    emitter.send(SseEmitter.event()
                            .id(cached.eventId())
                            .name(cached.eventType())
                            .data(cached.data()));
                }
                emitter.complete();
            } catch (IOException e) {
                log.debug("Reconnect replay failed: {}", e.getMessage());
            }
        });
        return emitter;
    }

    public SseEmitter handleContinue(UUID sessionId, UUID sessionStateId, UUID userId) {
        ChatSessionEntity session = sessionService.getSessionOrThrow(sessionId, userId);

        ChatSessionStateEntity state = sessionStateMapper.selectById(sessionStateId);
        if (state == null || !state.getSessionId().equals(sessionId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (state.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCode.CHAT_SESSION_STATE_EXPIRED,
                    Map.of("session_state_id", sessionStateId.toString()));
        }

        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String messageId = UUID.randomUUID().toString();
        int resumeStep = state.getStepCount();

        int maxSteps = resolveMaxSteps(session.getCurrentAgentId());

        return startStreamReply(session, requestId, messageId, maxSteps, resumeStep, false, null);
    }

    private SseEmitter startStreamReply(ChatSessionEntity session, String requestId,
                                         String messageId, int maxSteps, int startStep,
                                         boolean isUpdate, String idempotencyKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                SseEventBuilder evtBuilder = new SseEventBuilder(requestId, messageId, objectMapper);
                emitter.send(evtBuilder.heartbeat().sseEvent());
            } catch (IOException e) {
                log.debug("Heartbeat send failed (client may have disconnected)");
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        emitter.onCompletion(() -> heartbeat.cancel(false));
        emitter.onTimeout(() -> heartbeat.cancel(false));
        emitter.onError(e -> heartbeat.cancel(false));

        Thread.ofVirtual().name("chat-orch-" + messageId).start(() ->
                streamReply(session, requestId, messageId, maxSteps, startStep, isUpdate,
                        idempotencyKey, emitter));

        return emitter;
    }

    private void streamReply(ChatSessionEntity session, String requestId, String messageId,
                             int maxSteps, int startStep, boolean isUpdate,
                             String idempotencyKey, SseEmitter emitter) {
        SseEventBuilder evt = new SseEventBuilder(requestId, messageId, objectMapper);
        StringBuilder fullContent = new StringBuilder();
        List<Map<String, Object>> toolCallRecords = new ArrayList<>();
        List<Map<String, Object>> toolResultRecords = new ArrayList<>();
        int totalSteps = startStep;
        int promptTokens = 0;
        int completionTokens = 0;

        try {
            UUID agentId = session.getCurrentAgentId();
            String modelId = resolveModelId(agentId);
            Map<String, Map<String, Object>> toolBindingMap = agentConfigProvider.map(p -> p.getToolBindings(agentId)).orElse(Map.of());

            sseEventCacheService.ifPresent(svc -> svc.registerSessionMessage(session.getId().toString(), messageId));
            sendAndCache(emitter, evt.messageStart(agentId, modelId), messageId);

            List<LlmMessage> context = buildContext(session);

            for (int step = startStep + 1; step <= startStep + maxSteps; step++) {
                totalSteps = step;

                Iterator<LlmChunk> stream = llmStreamService.stream(modelId, context, List.of());
                boolean continueLoop = false;

                while (stream.hasNext()) {
                    LlmChunk chunk = stream.next();

                    switch (chunk) {
                        case LlmChunk.TokenChunk tc -> {
                            fullContent.append(tc.delta());
                            sendAndCache(emitter, evt.token(tc.delta()), messageId);
                        }
                        case LlmChunk.ToolCallChunk tcc -> {
                            sendAndCache(emitter, evt.toolCallStart(tcc.toolCallId(), tcc.toolName(),
                                    tcc.arguments(), step), messageId);

                            Map<String, Object> bindingConfig = toolBindingMap.getOrDefault(
                                    tcc.toolName(), Map.of());
                            ToolResult result = toolDispatcher.dispatch(
                                    tcc.toolCallId(), tcc.toolName(), tcc.arguments(), bindingConfig);

                            sendAndCache(emitter, evt.toolCallEnd(tcc.toolCallId(), result.status(),
                                    truncate(result.content(), 200), result.durationMs()),
                                    messageId);

                            toolCallRecords.add(Map.of(
                                    "tool_call_id", tcc.toolCallId(),
                                    "tool_name", tcc.toolName(),
                                    "arguments", tcc.arguments()));
                            toolResultRecords.add(Map.of(
                                    "tool_call_id", tcc.toolCallId(),
                                    "status", result.status(),
                                    "content", result.content()));

                            context.add(LlmMessage.toolResult(tcc.toolCallId(), result.content()));
                            continueLoop = true;
                        }
                        case LlmChunk.FinishChunk fc -> {
                            promptTokens += fc.promptTokens();
                            completionTokens += fc.completionTokens();
                            continueLoop = false;
                        }
                        case LlmChunk.ErrorChunk ec -> {
                            sendAndCache(emitter, evt.error(ec.code(), ec.message(), false),
                                    messageId);
                            persistMessage(session.getId(), messageId, fullContent.toString(),
                                    "incomplete", agentId, modelId, totalSteps,
                                    toolCallRecords, toolResultRecords, promptTokens, completionTokens,
                                    isUpdate);
                            emitter.complete();
                            return;
                        }
                    }
                }

                if (!continueLoop) {
                    break;
                }

                if (step >= startStep + maxSteps) {
                    UUID stateId = saveSessionState(session.getId(), context, step);
                    sendAndCache(emitter, evt.stepLimit(step, maxSteps, stateId), messageId);
                    persistMessage(session.getId(), messageId, fullContent.toString(),
                            "incomplete", agentId, modelId, totalSteps,
                            toolCallRecords, toolResultRecords, promptTokens, completionTokens,
                            isUpdate);
                    emitter.complete();
                    return;
                }
            }

            Map<String, Object> usage = Map.of(
                    "prompt_tokens", promptTokens,
                    "completion_tokens", completionTokens);
            sendAndCache(emitter, evt.messageEnd("stop", usage, totalSteps), messageId);

            persistMessage(session.getId(), messageId, fullContent.toString(),
                    "complete", agentId, modelId, totalSteps,
                    toolCallRecords, toolResultRecords, promptTokens, completionTokens,
                    isUpdate);

            if (idempotencyKey != null && idempotencyService.isPresent()) {
                idempotencyService.get().markComplete(session.getId(), idempotencyKey, messageId);
            }

            if (fullContent.length() > 0) {
                String title = fullContent.substring(0, Math.min(20, fullContent.length()));
                sessionService.updateSessionTitle(session.getId(), title);
            }

            emitter.complete();

        } catch (IOException e) {
            log.warn("SSE send failed (client disconnected): {}", e.getMessage());
        } catch (Exception e) {
            log.error("Chat orchestration error", e);
            try {
                emitter.send(evt.error("CHAT_MODEL_ERROR", e.getMessage(), false).sseEvent());
                emitter.complete();
            } catch (IOException ioe) {
                log.debug("Failed to send error event");
            }
        }
    }

    private List<LlmMessage> buildContext(ChatSessionEntity session) {
        List<LlmMessage> context = new ArrayList<>();

        // TODO: Load system_prompt from AgentService.getAgentConfig(agent_id)
        context.add(LlmMessage.system("You are a helpful AI assistant."));

        // Load message history, respecting separator boundaries
        List<ChatMessageEntity> history = loadRelevantHistory(session.getId());
        for (ChatMessageEntity msg : history) {
            switch (msg.getRole()) {
                case "user" -> context.add(LlmMessage.user(msg.getContent()));
                case "assistant" -> context.add(LlmMessage.assistant(msg.getContent()));
                case "system" -> context.add(LlmMessage.system(msg.getContent()));
                default -> {} // skip separators
            }
        }

        return context;
    }

    private List<ChatMessageEntity> loadRelevantHistory(UUID sessionId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .orderByAsc(ChatMessageEntity::getCreatedAt);
        List<ChatMessageEntity> all = messageMapper.selectList(wrapper);

        // Only include messages after the last separator (Agent switch boundary)
        int lastSeparatorIdx = -1;
        for (int i = all.size() - 1; i >= 0; i--) {
            if ("separator".equals(all.get(i).getRole())) {
                lastSeparatorIdx = i;
                break;
            }
        }
        if (lastSeparatorIdx >= 0) {
            return all.subList(lastSeparatorIdx + 1, all.size());
        }
        return all;
    }

    private String resolveModelId(UUID agentId) {
        String modelId = agentConfigProvider.map(p -> p.getModelId(agentId)).orElse(null);
        return modelId != null ? modelId : DEFAULT_MODEL_ID;
    }

    private int resolveMaxSteps(UUID agentId) {
        Integer maxSteps = agentConfigProvider.map(p -> p.getMaxSteps(agentId)).orElse(null);
        return maxSteps != null ? maxSteps : DEFAULT_MAX_STEPS;
    }

    private void sendAndCache(SseEmitter emitter, SseEventWithMeta event,
                              String messageId) throws IOException {
        emitter.send(event.sseEvent());
        try {
            sseEventCacheService.ifPresent(svc -> svc.appendEvent(messageId, event.eventId(),
                    event.eventType(), event.jsonData()));
        } catch (Exception e) {
            log.debug("SSE event cache failed: {}", e.getMessage());
        }
    }

    private SseEmitter replayCachedEvents(String messageId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Thread.ofVirtual().name("sse-replay-" + messageId).start(() -> {
            try {
                var events = sseEventCacheService.map(svc -> svc.getEventsAfter(messageId, null)).orElse(List.of());
                for (var cached : events) {
                    emitter.send(SseEmitter.event()
                            .id(cached.eventId())
                            .name(cached.eventType())
                            .data(cached.data()));
                }
                emitter.complete();
            } catch (IOException e) {
                log.debug("Replay send failed: {}", e.getMessage());
            }
        });
        return emitter;
    }

    private UUID saveSessionState(UUID sessionId, List<LlmMessage> context, int stepCount) {
        UUID stateId = UUID.randomUUID();
        ChatSessionStateEntity state = new ChatSessionStateEntity()
                .setId(stateId)
                .setSessionId(sessionId)
                .setContextSnapshot(toJson(context))
                .setStepCount(stepCount)
                .setCreatedAt(OffsetDateTime.now())
                .setExpiresAt(OffsetDateTime.now().plusHours(1));
        sessionStateMapper.insert(state);
        return stateId;
    }

    private void persistMessage(UUID sessionId, String messageId, String content, String status,
                                UUID agentId, String modelId, int stepCount,
                                List<Map<String, Object>> toolCalls,
                                List<Map<String, Object>> toolResults,
                                int promptTokens, int completionTokens,
                                boolean isUpdate) {
        String toolCallsJson = toolCalls.isEmpty() ? null : toJson(toolCalls);
        String toolResultsJson = toolResults.isEmpty() ? null : toJson(toolResults);
        Map<String, Object> usage = Map.of(
                "prompt_tokens", promptTokens,
                "completion_tokens", completionTokens);
        String usageJson = toJson(usage);

        if (isUpdate) {
            UUID msgUuid = UUID.fromString(messageId);
            ChatMessageEntity existing = messageMapper.selectById(msgUuid);
            if (existing != null) {
                existing.setContent(content);
                existing.setStatus(status);
                existing.setModelId(modelId);
                existing.setStepCount(stepCount);
                existing.setToolCalls(toolCallsJson);
                existing.setToolResults(toolResultsJson);
                existing.setUsage(usageJson);
                messageMapper.updateById(existing);
                return;
            }
        }

        ChatMessageEntity msg = new ChatMessageEntity()
                .setId(UUID.fromString(messageId))
                .setSessionId(sessionId)
                .setRole("assistant")
                .setContent(content)
                .setStatus(status)
                .setAgentId(agentId)
                .setModelId(modelId)
                .setStepCount(stepCount)
                .setCreatedAt(OffsetDateTime.now());

        msg.setToolCalls(toolCallsJson);
        msg.setToolResults(toolResultsJson);
        msg.setUsage(usageJson);

        messageMapper.insert(msg);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON serialization failed", e);
            return "{}";
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
