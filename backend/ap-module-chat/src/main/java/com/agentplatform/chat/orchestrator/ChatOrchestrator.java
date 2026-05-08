package com.agentplatform.chat.orchestrator;

import com.agentplatform.chat.dto.SendMessageRequest;
import com.agentplatform.chat.dto.QuestionAnswerRequest;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.entity.ChatSessionStateEntity;
import com.agentplatform.chat.llm.LlmChunk;
import com.agentplatform.chat.llm.LlmMessage;
import com.agentplatform.chat.llm.LlmMessage.LlmToolCall;
import com.agentplatform.chat.llm.LlmStreamService;
import com.agentplatform.chat.llm.LlmToolSpecFactory;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionStateMapper;
import com.agentplatform.chat.service.ChatSessionService;
import com.agentplatform.chat.sse.IdempotencyService;
import com.agentplatform.chat.sse.SseEventBuilder;
import com.agentplatform.chat.sse.SseEventBuilder.SseEventWithMeta;
import com.agentplatform.chat.sse.SseEventCacheService;
import com.agentplatform.chat.tool.BuiltinToolExecutor;
import com.agentplatform.chat.tool.BuiltinToolExecutor.PendingQuestion;
import com.agentplatform.chat.tool.BuiltinToolExecutor.TodoState;
import com.agentplatform.chat.tool.ToolDispatcher;
import com.agentplatform.common.core.tool.BuiltinUiTools;
import com.agentplatform.common.core.tool.ToolResult;
import com.agentplatform.chat.tool.BuiltinToolExecutor.QuestionOption;
import com.agentplatform.common.core.agent.AgentConfigProvider;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
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
    private static final String BUILTIN_UI_TOOL_INSTRUCTIONS = """
            Runtime tool instructions:
            - When you need the user's answer before continuing, call the `question` tool instead of writing the question as assistant text.
            - By default, call `question` with 3-6 concise answer options and allow_free_text=true so the UI shows the options plus one open-ended free-text answer field.
            - If the user asks you to ask multiple questions, call `question` for exactly one question, wait for the tool result, then continue with the next question until the requested count is complete.
            """;

    private final ChatSessionService sessionService;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionStateMapper sessionStateMapper;
    private final LlmStreamService llmStreamService;
    private final ToolDispatcher toolDispatcher;
    private final BuiltinToolExecutor builtinToolExecutor;
    private final LlmToolSpecFactory llmToolSpecFactory;
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
                            BuiltinToolExecutor builtinToolExecutor,
                            LlmToolSpecFactory llmToolSpecFactory,
                            ObjectMapper objectMapper,
                            Optional<AgentConfigProvider> agentConfigProvider,
                            Optional<IdempotencyService> idempotencyService,
                            Optional<SseEventCacheService> sseEventCacheService) {
        this.sessionService = sessionService;
        this.messageMapper = messageMapper;
        this.sessionStateMapper = sessionStateMapper;
        this.llmStreamService = llmStreamService;
        this.toolDispatcher = toolDispatcher;
        this.builtinToolExecutor = builtinToolExecutor;
        this.llmToolSpecFactory = llmToolSpecFactory;
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
                request.getIdempotencyKey(), null);
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

        return startStreamReply(session, requestId, messageId, maxSteps, 0, true, null, null);
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
        int resumeStep = state.getStepCount();

        int maxSteps = resolveMaxSteps(session.getCurrentAgentId());

        ResumeData resume = resumeDataFromState(state, null);
        String messageId = resume.messageId() != null ? resume.messageId() : UUID.randomUUID().toString();
        return startStreamReply(session, requestId, messageId, maxSteps, resumeStep, true, null, resume);
    }

    public SseEmitter handleQuestionAnswer(UUID sessionId, UUID sessionStateId,
                                           QuestionAnswerRequest request, UUID userId) {
        ChatSessionEntity session = sessionService.getSessionOrThrow(sessionId, userId);

        ChatSessionStateEntity state = sessionStateMapper.selectById(sessionStateId);
        if (state == null || !state.getSessionId().equals(sessionId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (state.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCode.CHAT_SESSION_STATE_EXPIRED,
                    Map.of("session_state_id", sessionStateId.toString()));
        }

        Map<String, Object> toolCache = parseJsonMap(state.getToolCache());
        PendingQuestion pending = pendingQuestionFromRaw(toolCache.get("pending_question"));
        if (pending == null || !Objects.equals(pending.questionId(), request.getQuestionId())) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Question does not match pending state"));
        }

        String answerResult = builtinToolExecutor.buildQuestionAnswerResult(
                pending, request.getSelectedOptionIds(), request.getAnswerText());
        ResumeData resume = resumeDataFromState(state,
                new QuestionAnswer(pending.toolCallId(), answerResult));

        String answeredMessageId = resume.messageId();
        if (answeredMessageId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Pending question state is missing message_id"));
        }
        markQuestionAnswered(answeredMessageId, resume.questionAnswer());

        String messageId = UUID.randomUUID().toString();
        ResumeData continuation = new ResumeData(
                resume.context(),
                resume.todoState(),
                resume.questionAnswer(),
                messageId,
                "",
                new ArrayList<>(),
                new ArrayList<>());
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int maxSteps = resolveMaxSteps(session.getCurrentAgentId());
        return startStreamReply(session, requestId, messageId, maxSteps, state.getStepCount(),
                false, null, continuation);
    }

    private SseEmitter startStreamReply(ChatSessionEntity session, String requestId,
                                         String messageId, int maxSteps, int startStep,
                                         boolean isUpdate, String idempotencyKey,
                                         ResumeData resumeData) {
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
                        idempotencyKey, resumeData, emitter));

        return emitter;
    }

    private void streamReply(ChatSessionEntity session, String requestId, String messageId,
                             int maxSteps, int startStep, boolean isUpdate,
                             String idempotencyKey, ResumeData resumeData, SseEmitter emitter) {
        SseEventBuilder evt = new SseEventBuilder(requestId, messageId, objectMapper);
        StringBuilder fullContent = new StringBuilder(resumeData != null && resumeData.initialContent() != null
                ? resumeData.initialContent() : "");
        List<Map<String, Object>> toolCallRecords = new ArrayList<>(
                resumeData != null ? resumeData.toolCallRecords() : List.of());
        List<Map<String, Object>> toolResultRecords = new ArrayList<>(
                resumeData != null ? resumeData.toolResultRecords() : List.of());
        TodoState todoState = resumeData != null ? resumeData.todoState() : null;
        int totalSteps = startStep;
        int promptTokens = 0;
        int completionTokens = 0;
        String finalFinishReason = "stop";

        try {
            UUID agentId = session.getCurrentAgentId();
            String modelId = resolveModelId(agentId);
            Map<String, Map<String, Object>> toolBindingMap = agentConfigProvider.map(p -> p.getToolBindings(agentId)).orElse(Map.of());
            List<Map<String, Object>> tools = llmToolSpecFactory.buildTools(toolBindingMap);

            sseEventCacheService.ifPresent(svc -> svc.registerSessionMessage(session.getId().toString(), messageId));
            sendAndCache(emitter, evt.messageStart(agentId, modelId), messageId);

            List<LlmMessage> context = resumeData != null
                    ? new ArrayList<>(resumeData.context())
                    : buildContext(session);
            if (resumeData != null && resumeData.questionAnswer() != null) {
                QuestionAnswer answer = resumeData.questionAnswer();
                context.add(LlmMessage.toolResult(answer.toolCallId(), answer.content()));
                toolResultRecords.add(Map.of(
                        "tool_call_id", answer.toolCallId(),
                        "status", "success",
                        "content", answer.content(),
                        "builtin_ui", BuiltinUiTools.QUESTION));
            }

            for (int step = startStep + 1; step <= startStep + maxSteps; step++) {
                totalSteps = step;

                Iterator<LlmChunk> stream = llmStreamService.stream(modelId, context, tools);
                List<LlmChunk.ToolCallChunk> stepToolCalls = new ArrayList<>();
                StringBuilder assistantStepContent = new StringBuilder();
                StringBuilder assistantStepReasoning = new StringBuilder();

                while (stream.hasNext()) {
                    LlmChunk chunk = stream.next();

                    switch (chunk) {
                        case LlmChunk.ReasoningChunk rc -> assistantStepReasoning.append(rc.delta());
                        case LlmChunk.TokenChunk tc -> {
                            fullContent.append(tc.delta());
                            assistantStepContent.append(tc.delta());
                            sendAndCache(emitter, evt.token(tc.delta()), messageId);
                        }
                        case LlmChunk.ToolCallChunk tcc -> stepToolCalls.add(tcc);
                        case LlmChunk.FinishChunk fc -> {
                            promptTokens += fc.promptTokens();
                            completionTokens += fc.completionTokens();
                            finalFinishReason = fc.finishReason() != null ? fc.finishReason() : "stop";
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

                if (stepToolCalls.isEmpty()) {
                    break;
                }

                List<LlmChunk.ToolCallChunk> normalizedToolCalls = stepToolCalls.stream()
                        .map(t -> new LlmChunk.ToolCallChunk(
                                t.toolCallId() == null || t.toolCallId().isBlank()
                                        ? "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                                        : t.toolCallId(),
                                t.toolName(),
                                t.arguments()))
                        .toList();

                context.add(LlmMessage.assistantToolCalls(
                        assistantStepContent.toString().isBlank() ? null : assistantStepContent.toString(),
                        normalizedToolCalls.stream()
                                .map(t -> new LlmToolCall(t.toolCallId(), t.toolName(), t.arguments()))
                                .toList(),
                        assistantStepReasoning.toString().isBlank() ? null : assistantStepReasoning.toString()));

                for (LlmChunk.ToolCallChunk tcc : normalizedToolCalls) {
                    String toolCallId = tcc.toolCallId();
                    sendAndCache(emitter, evt.toolCallStart(toolCallId, tcc.toolName(),
                            tcc.arguments(), step), messageId);

                    toolCallRecords.add(Map.of(
                            "tool_call_id", toolCallId,
                            "tool_name", tcc.toolName(),
                            "arguments", tcc.arguments()));

                    if (BuiltinUiTools.TODO.equals(tcc.toolName())) {
                        long start = System.currentTimeMillis();
                        todoState = builtinToolExecutor.applyTodo(tcc.arguments(), todoState);
                        List<Map<String, Object>> items = todoItemsAsMaps(todoState);
                        sendAndCache(emitter, evt.todoUpdated(toolCallId, todoState.title(), items),
                                messageId);
                        String resultContent = toJson(todoResultMap(todoState));
                        sendAndCache(emitter, evt.toolCallEnd(toolCallId, "success",
                                        "Todo list updated", System.currentTimeMillis() - start),
                                messageId);
                        toolResultRecords.add(Map.of(
                                "tool_call_id", toolCallId,
                                "status", "success",
                                "content", resultContent,
                                "todo_state", todoStateAsMap(todoState)));
                        context.add(LlmMessage.toolResult(toolCallId, resultContent));
                        continue;
                    }

                    if (BuiltinUiTools.QUESTION.equals(tcc.toolName())) {
                        long start = System.currentTimeMillis();
                        PendingQuestion pendingQuestion;
                        try {
                            pendingQuestion = builtinToolExecutor.parseQuestion(toolCallId, tcc.arguments());
                        } catch (BizException e) {
                            String validationMessage = "Invalid question tool call. Call `question` again with "
                                    + "3-6 answer options and allow_free_text=true so the UI shows options plus "
                                    + "one open-ended answer field.";
                            sendAndCache(emitter, evt.toolCallEnd(toolCallId, "failed",
                                            validationMessage, System.currentTimeMillis() - start),
                                    messageId);
                            toolResultRecords.add(Map.of(
                                    "tool_call_id", toolCallId,
                                    "status", "failed",
                                    "content", validationMessage,
                                    "builtin_ui", BuiltinUiTools.QUESTION));
                            context.add(LlmMessage.toolResult(toolCallId, validationMessage));
                            continue;
                        }
                        Map<String, Object> questionState = pendingQuestionAsMap(pendingQuestion);
                        UUID stateId = UUID.randomUUID();
                        questionState.put("session_state_id", stateId.toString());
                        toolResultRecords.add(Map.of(
                                "tool_call_id", toolCallId,
                                "status", "requires_action",
                                "content", toJson(questionState),
                                "question_state", questionState));
                        saveSessionState(stateId, session.getId(), context, step,
                                toolCache("question", messageId, questionState, todoState,
                                        toolCallRecords, toolResultRecords));
                        sendAndCache(emitter, evt.toolCallEnd(toolCallId, "requires_action",
                                        "Waiting for user answer", System.currentTimeMillis() - start),
                                messageId);
                        sendAndCache(emitter, evt.question(toolCallId, stateId,
                                        pendingQuestion.questionId(), pendingQuestion.question(),
                                        questionOptionsAsMaps(pendingQuestion),
                                        pendingQuestion.allowFreeText(), pendingQuestion.multiSelect()),
                                messageId);
                        persistMessage(session.getId(), messageId, fullContent.toString(),
                                "incomplete", agentId, modelId, totalSteps,
                                toolCallRecords, toolResultRecords, promptTokens, completionTokens,
                                isUpdate);
                        emitter.complete();
                        return;
                    }

                    Map<String, Object> bindingConfig = toolBindingMap.getOrDefault(
                            tcc.toolName(), Map.of());
                    ToolResult result = toolDispatcher.dispatch(
                            toolCallId, tcc.toolName(), tcc.arguments(), bindingConfig);

                    sendAndCache(emitter, evt.toolCallEnd(toolCallId, result.status(),
                                    truncate(result.content(), 200), result.durationMs()),
                            messageId);

                    toolResultRecords.add(Map.of(
                            "tool_call_id", toolCallId,
                            "status", result.status(),
                            "content", result.content()));
                    context.add(LlmMessage.toolResult(toolCallId, result.content()));
                }

                if (step >= startStep + maxSteps) {
                    UUID stateId = saveSessionState(session.getId(), context, step,
                            toolCache("step_limit", messageId, null, todoState,
                                    toolCallRecords, toolResultRecords));
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
            sendAndCache(emitter, evt.messageEnd(finalFinishReason, usage, totalSteps), messageId);

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

        String systemPrompt = agentConfigProvider
                .map(p -> p.getSystemPrompt(session.getCurrentAgentId()))
                .orElse(null);
        context.add(LlmMessage.system(withBuiltinUiToolInstructions(
                systemPrompt != null && !systemPrompt.isBlank()
                        ? systemPrompt
                        : "You are a helpful AI assistant.")));

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

    private String withBuiltinUiToolInstructions(String systemPrompt) {
        return systemPrompt + "\n\n" + BUILTIN_UI_TOOL_INSTRUCTIONS;
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

    private ResumeData resumeDataFromState(ChatSessionStateEntity state, QuestionAnswer questionAnswer) {
        List<LlmMessage> context = parseContextSnapshot(state.getContextSnapshot());
        Map<String, Object> toolCache = parseJsonMap(state.getToolCache());
        String messageId = stringValue(toolCache.get("message_id"));
        TodoState todoState = todoStateFromRaw(toolCache.get("todo_state"));
        List<Map<String, Object>> toolCalls = parseMapList(toolCache.get("tool_calls"));
        List<Map<String, Object>> toolResults = parseMapList(toolCache.get("tool_results"));
        String initialContent = "";

        if (messageId != null) {
            try {
                ChatMessageEntity existing = messageMapper.selectById(UUID.fromString(messageId));
                if (existing != null) {
                    initialContent = existing.getContent() != null ? existing.getContent() : "";
                    if (toolCalls.isEmpty()) {
                        toolCalls = parseMapList(existing.getToolCalls());
                    }
                    if (toolResults.isEmpty()) {
                        toolResults = parseMapList(existing.getToolResults());
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // fall through with empty initial content
            }
        }

        return new ResumeData(context, todoState, questionAnswer, messageId,
                initialContent, toolCalls, toolResults);
    }

    private List<LlmMessage> parseContextSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse context snapshot: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON map: {}", e.getMessage());
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseMapList(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        try {
            if (raw instanceof String s) {
                if (s.isBlank()) {
                    return new ArrayList<>();
                }
                return objectMapper.readValue(s, new TypeReference<>() {});
            }
            if (raw instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse map list: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private TodoState todoStateFromRaw(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(raw, TodoState.class);
        } catch (Exception e) {
            log.warn("Failed to parse todo state: {}", e.getMessage());
            return null;
        }
    }

    private PendingQuestion pendingQuestionFromRaw(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(raw, new TypeReference<>() {});
            List<Map<String, Object>> optionMaps = parseMapList(map.get("options"));
            List<QuestionOption> options = optionMaps.stream()
                    .map(option -> new QuestionOption(
                            stringValue(option.get("id")),
                            stringValue(option.get("label")),
                            stringValue(option.get("description"))))
                    .toList();
            return new PendingQuestion(
                    firstString(map, "questionId", "question_id"),
                    firstString(map, "toolCallId", "tool_call_id"),
                    stringValue(map.get("question")),
                    options,
                    booleanValue(firstRaw(map, "allowFreeText", "allow_free_text")),
                    booleanValue(firstRaw(map, "multiSelect", "multi_select")));
        } catch (Exception e) {
            log.warn("Failed to parse pending question: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> toolCache(String reason, String messageId,
                                          Map<String, Object> pendingQuestion,
                                          TodoState todoState,
                                          List<Map<String, Object>> toolCalls,
                                          List<Map<String, Object>> toolResults) {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("resume_reason", reason);
        cache.put("message_id", messageId);
        if (pendingQuestion != null) {
            cache.put("pending_question", pendingQuestion);
        }
        if (todoState != null) {
            cache.put("todo_state", todoStateAsMap(todoState));
        }
        cache.put("tool_calls", toolCalls);
        cache.put("tool_results", toolResults);
        return cache;
    }

    private Map<String, Object> todoResultMap(TodoState todoState) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "updated");
        result.put("title", todoState.title());
        result.put("items", todoItemsAsMaps(todoState));
        return result;
    }

    private Map<String, Object> todoStateAsMap(TodoState todoState) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("title", todoState.title());
        state.put("items", todoItemsAsMaps(todoState));
        return state;
    }

    private List<Map<String, Object>> todoItemsAsMaps(TodoState todoState) {
        if (todoState == null || todoState.items() == null) {
            return List.of();
        }
        return todoState.items().stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.id());
            map.put("title", item.title());
            map.put("status", item.status());
            if (item.detail() != null) {
                map.put("detail", item.detail());
            }
            return map;
        }).toList();
    }

    private Map<String, Object> pendingQuestionAsMap(PendingQuestion pendingQuestion) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("question_id", pendingQuestion.questionId());
        map.put("tool_call_id", pendingQuestion.toolCallId());
        map.put("question", pendingQuestion.question());
        map.put("options", questionOptionsAsMaps(pendingQuestion));
        map.put("allow_free_text", pendingQuestion.allowFreeText());
        map.put("multi_select", pendingQuestion.multiSelect());
        return map;
    }

    private List<Map<String, Object>> questionOptionsAsMaps(PendingQuestion pendingQuestion) {
        return pendingQuestion.options().stream().map(option -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", option.id());
            map.put("label", option.label());
            if (option.description() != null) {
                map.put("description", option.description());
            }
            return map;
        }).toList();
    }

    private UUID saveSessionState(UUID sessionId, List<LlmMessage> context, int stepCount,
                                  Map<String, Object> toolCache) {
        UUID stateId = UUID.randomUUID();
        saveSessionState(stateId, sessionId, context, stepCount, toolCache);
        return stateId;
    }

    private void saveSessionState(UUID stateId, UUID sessionId, List<LlmMessage> context,
                                  int stepCount, Map<String, Object> toolCache) {
        ChatSessionStateEntity state = new ChatSessionStateEntity()
                .setId(stateId)
                .setSessionId(sessionId)
                .setContextSnapshot(toJson(context))
                .setStepCount(stepCount)
                .setToolCache(toJson(toolCache))
                .setCreatedAt(OffsetDateTime.now())
                .setExpiresAt(OffsetDateTime.now().plusHours(1));
        sessionStateMapper.insert(state);
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

    private void markQuestionAnswered(String messageId, QuestionAnswer answer) {
        ChatMessageEntity existing;
        try {
            existing = messageMapper.selectById(UUID.fromString(messageId));
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Pending question state has invalid message_id"));
        }
        if (existing == null) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }

        List<Map<String, Object>> toolResults = parseMapList(existing.getToolResults());
        boolean alreadyRecorded = toolResults.stream().anyMatch(result ->
                Objects.equals(answer.toolCallId(), stringValue(result.get("tool_call_id")))
                        && "success".equalsIgnoreCase(stringValue(result.get("status")))
                        && BuiltinUiTools.QUESTION.equals(stringValue(result.get("builtin_ui"))));
        if (!alreadyRecorded) {
            Map<String, Object> answerResult = new LinkedHashMap<>();
            answerResult.put("tool_call_id", answer.toolCallId());
            answerResult.put("status", "success");
            answerResult.put("content", answer.content());
            answerResult.put("builtin_ui", BuiltinUiTools.QUESTION);
            toolResults.add(answerResult);
        }

        existing.setStatus("complete");
        existing.setToolResults(toolResults.isEmpty() ? null : toJson(toolResults));
        messageMapper.updateById(existing);
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

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Object firstRaw(Map<String, Object> map, String firstKey, String secondKey) {
        Object value = map.get(firstKey);
        return value != null ? value : map.get(secondKey);
    }

    private String firstString(Map<String, Object> map, String firstKey, String secondKey) {
        return stringValue(firstRaw(map, firstKey, secondKey));
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private record QuestionAnswer(String toolCallId, String content) {}

    private record ResumeData(
            List<LlmMessage> context,
            TodoState todoState,
            QuestionAnswer questionAnswer,
            String messageId,
            String initialContent,
            List<Map<String, Object>> toolCallRecords,
            List<Map<String, Object>> toolResultRecords) {}
}
