package com.agentplatform.chat.orchestrator;

import com.agentplatform.chat.dto.SendMessageRequest;
import com.agentplatform.chat.dto.QuestionAnswerRequest;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.entity.ChatSessionStateEntity;
import com.agentplatform.chat.llm.DefaultLlmStreamService;
import com.agentplatform.chat.llm.LlmChunk;
import com.agentplatform.chat.llm.LlmToolSpecFactory;
import com.agentplatform.chat.llm.LlmStreamService;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionStateMapper;
import com.agentplatform.chat.service.ChatSessionService;
import com.agentplatform.chat.sse.IdempotencyService;
import com.agentplatform.chat.sse.SseEventCacheService;
import com.agentplatform.chat.tool.BuiltinToolExecutor;
import com.agentplatform.chat.tool.ToolDispatcher;
import com.agentplatform.common.core.agent.AgentConfigProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatOrchestrator — 4.4 Core Loop")
class ChatOrchestratorTest {

    @Mock private ChatSessionService sessionService;
    @Mock private ChatMessageMapper messageMapper;
    @Mock private ChatSessionStateMapper sessionStateMapper;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private AgentConfigProvider agentConfigProvider;
    @Mock private IdempotencyService idempotencyService;
    @Mock private SseEventCacheService sseEventCacheService;

    private LlmStreamService llmStreamService;
    private ObjectMapper objectMapper;
    private ChatOrchestrator orchestrator;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID AGENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        llmStreamService = new DefaultLlmStreamService();
        orchestrator = new ChatOrchestrator(
                sessionService, messageMapper, sessionStateMapper,
                llmStreamService, toolDispatcher,
                new BuiltinToolExecutor(objectMapper), new LlmToolSpecFactory(objectMapper),
                objectMapper,
                Optional.of(agentConfigProvider), Optional.of(idempotencyService), Optional.of(sseEventCacheService));
    }

    @Test
    @DisplayName("handleSendMessage returns SseEmitter and persists user message")
    void sendMessageReturnsSseEmitter() {
        ChatSessionEntity session = new ChatSessionEntity()
                .setId(SESSION_ID)
                .setUserId(USER_ID)
                .setCurrentAgentId(AGENT_ID)
                .setCreatedAt(OffsetDateTime.now());
        when(sessionService.getSessionOrThrow(SESSION_ID, USER_ID)).thenReturn(session);

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello");

        SseEmitter emitter = orchestrator.handleSendMessage(SESSION_ID, request, USER_ID);

        assertThat(emitter).isNotNull();
        verify(messageMapper).insert(any(ChatMessageEntity.class));
    }

    @Test
    @DisplayName("emitter has 5-minute timeout")
    void emitterTimeout() {
        ChatSessionEntity session = new ChatSessionEntity()
                .setId(SESSION_ID)
                .setUserId(USER_ID)
                .setCurrentAgentId(AGENT_ID)
                .setCreatedAt(OffsetDateTime.now());
        when(sessionService.getSessionOrThrow(SESSION_ID, USER_ID)).thenReturn(session);

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello");

        SseEmitter emitter = orchestrator.handleSendMessage(SESSION_ID, request, USER_ID);
        assertThat(emitter.getTimeout()).isEqualTo(300000L);
    }

    @Test
    @DisplayName("answering a question stores continuation in a new assistant message")
    void answerQuestionCreatesNewAssistantMessage() throws Exception {
        UUID stateId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID answeredMessageId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        String toolCallId = "call_question_1";

        ChatSessionEntity session = new ChatSessionEntity()
                .setId(SESSION_ID)
                .setUserId(USER_ID)
                .setCurrentAgentId(AGENT_ID)
                .setCreatedAt(OffsetDateTime.now());
        when(sessionService.getSessionOrThrow(SESSION_ID, USER_ID)).thenReturn(session);

        Map<String, Object> questionState = new LinkedHashMap<>();
        questionState.put("question_id", "q1");
        questionState.put("tool_call_id", toolCallId);
        questionState.put("question", "Pick one");
        questionState.put("options", List.of(
                Map.of("id", "a", "label", "A"),
                Map.of("id", "b", "label", "B"),
                Map.of("id", "c", "label", "C")));
        questionState.put("allow_free_text", true);
        questionState.put("multi_select", false);
        questionState.put("session_state_id", stateId.toString());

        List<Map<String, Object>> existingToolCalls = List.of(Map.of(
                "tool_call_id", toolCallId,
                "tool_name", "question",
                "arguments", objectMapper.writeValueAsString(Map.of(
                        "question", "Pick one",
                        "options", questionState.get("options")))));
        List<Map<String, Object>> existingToolResults = new ArrayList<>();
        existingToolResults.add(Map.of(
                "tool_call_id", toolCallId,
                "status", "requires_action",
                "content", objectMapper.writeValueAsString(questionState),
                "question_state", questionState));

        Map<String, Object> toolCache = new LinkedHashMap<>();
        toolCache.put("resume_reason", "question");
        toolCache.put("message_id", answeredMessageId.toString());
        toolCache.put("pending_question", questionState);
        toolCache.put("tool_calls", existingToolCalls);
        toolCache.put("tool_results", existingToolResults);

        ChatSessionStateEntity state = new ChatSessionStateEntity()
                .setId(stateId)
                .setSessionId(SESSION_ID)
                .setContextSnapshot("[]")
                .setStepCount(1)
                .setToolCache(objectMapper.writeValueAsString(toolCache))
                .setCreatedAt(OffsetDateTime.now())
                .setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        when(sessionStateMapper.selectById(eq(stateId))).thenReturn(state);

        ChatMessageEntity existing = new ChatMessageEntity()
                .setId(answeredMessageId)
                .setSessionId(SESSION_ID)
                .setRole("assistant")
                .setContent("First question")
                .setStatus("incomplete")
                .setToolCalls(objectMapper.writeValueAsString(existingToolCalls))
                .setToolResults(objectMapper.writeValueAsString(existingToolResults))
                .setAgentId(AGENT_ID)
                .setCreatedAt(OffsetDateTime.now());
        when(messageMapper.selectById(eq(answeredMessageId))).thenReturn(existing);
        when(agentConfigProvider.getMaxSteps(AGENT_ID)).thenReturn(10);
        when(agentConfigProvider.getToolBindings(AGENT_ID)).thenReturn(Map.of());

        LlmStreamService continuationLlm = (modelId, context, tools) -> List.<LlmChunk>of(
                new LlmChunk.TokenChunk("Thanks for answering."),
                new LlmChunk.FinishChunk("stop", 4, 8)).iterator();
        ChatOrchestrator continuationOrchestrator = new ChatOrchestrator(
                sessionService, messageMapper, sessionStateMapper,
                continuationLlm, toolDispatcher,
                new BuiltinToolExecutor(objectMapper), new LlmToolSpecFactory(objectMapper),
                objectMapper,
                Optional.of(agentConfigProvider), Optional.of(idempotencyService), Optional.of(sseEventCacheService));

        QuestionAnswerRequest request = new QuestionAnswerRequest();
        request.setQuestionId("q1");
        request.setSelectedOptionIds(List.of("a"));

        SseEmitter emitter = continuationOrchestrator.handleQuestionAnswer(
                SESSION_ID, stateId, request, USER_ID);

        assertThat(emitter).isNotNull();

        ArgumentCaptor<ChatMessageEntity> updateCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(messageMapper, timeout(3000).atLeastOnce()).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).anySatisfy(updated -> {
            assertThat(updated.getId()).isEqualTo(answeredMessageId);
            assertThat(updated.getStatus()).isEqualTo("complete");
            assertThat(updated.getToolResults()).contains("\"builtin_ui\":\"question\"");
            assertThat(updated.getToolResults()).contains("\"status\":\"success\"");
        });

        ArgumentCaptor<ChatMessageEntity> insertCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(messageMapper, timeout(3000).atLeastOnce()).insert(insertCaptor.capture());
        assertThat(insertCaptor.getAllValues()).anySatisfy(inserted -> {
            assertThat(inserted.getRole()).isEqualTo("assistant");
            assertThat(inserted.getId()).isNotEqualTo(answeredMessageId);
            assertThat(inserted.getContent()).isEqualTo("Thanks for answering.");
            assertThat(inserted.getStatus()).isEqualTo("complete");
        });
    }
}
