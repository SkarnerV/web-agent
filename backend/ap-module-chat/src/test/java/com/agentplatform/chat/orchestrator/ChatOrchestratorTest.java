package com.agentplatform.chat.orchestrator;

import com.agentplatform.chat.dto.SendMessageRequest;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.llm.DefaultLlmStreamService;
import com.agentplatform.chat.llm.LlmStreamService;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionStateMapper;
import com.agentplatform.chat.service.ChatSessionService;
import com.agentplatform.chat.sse.IdempotencyService;
import com.agentplatform.chat.sse.SseEventCacheService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                llmStreamService, toolDispatcher, objectMapper,
                agentConfigProvider, idempotencyService, sseEventCacheService);
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
}
