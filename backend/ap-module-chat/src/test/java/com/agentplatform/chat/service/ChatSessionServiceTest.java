package com.agentplatform.chat.service;

import com.agentplatform.chat.converter.ChatConverter;
import com.agentplatform.chat.dto.ChatSessionVO;
import com.agentplatform.chat.dto.CreateSessionRequest;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionService — 4.1 Session Management")
class ChatSessionServiceTest {

    @Mock private ChatSessionMapper sessionMapper;
    @Mock private ChatMessageMapper messageMapper;
    @Mock private ChatConverter chatConverter;

    private ChatSessionService service;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SESSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        service = new ChatSessionService(sessionMapper, messageMapper, chatConverter);
    }

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("creates session with correct agent_id and user_id")
        void createsSessionCorrectly() {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setAgentId(AGENT_ID);

            ChatSessionVO expectedVO = new ChatSessionVO();
            expectedVO.setId(SESSION_ID);
            when(chatConverter.toSessionVO(any(ChatSessionEntity.class))).thenReturn(expectedVO);

            ChatSessionVO result = service.createSession(request, USER_ID);

            ArgumentCaptor<ChatSessionEntity> captor = ArgumentCaptor.forClass(ChatSessionEntity.class);
            verify(sessionMapper).insert(captor.capture());

            ChatSessionEntity saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getCurrentAgentId()).isEqualTo(AGENT_ID);
            assertThat(saved.getTitle()).isEqualTo("新对话");
            assertThat(result).isEqualTo(expectedVO);
        }
    }

    @Nested
    @DisplayName("getSessionOrThrow")
    class GetSession {

        @Test
        @DisplayName("returns session when found and owned by user")
        void returnsSession() {
            ChatSessionEntity entity = new ChatSessionEntity()
                    .setId(SESSION_ID)
                    .setUserId(USER_ID);
            when(sessionMapper.selectById(SESSION_ID)).thenReturn(entity);

            ChatSessionEntity result = service.getSessionOrThrow(SESSION_ID, USER_ID);
            assertThat(result.getId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("throws CHAT_SESSION_NOT_FOUND when session does not exist")
        void throwsWhenNotFound() {
            when(sessionMapper.selectById(SESSION_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.getSessionOrThrow(SESSION_ID, USER_ID))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                            .isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND));
        }

        @Test
        @DisplayName("throws CHAT_SESSION_NOT_FOUND when session belongs to another user")
        void throwsWhenWrongUser() {
            UUID otherUser = UUID.randomUUID();
            ChatSessionEntity entity = new ChatSessionEntity()
                    .setId(SESSION_ID)
                    .setUserId(otherUser);
            when(sessionMapper.selectById(SESSION_ID)).thenReturn(entity);

            assertThatThrownBy(() -> service.getSessionOrThrow(SESSION_ID, USER_ID))
                    .isInstanceOf(BizException.class);
        }
    }

    @Nested
    @DisplayName("clearMessages")
    class ClearMessages {

        @Test
        @DisplayName("deletes all messages but preserves session")
        void clearsMessages() {
            ChatSessionEntity entity = new ChatSessionEntity()
                    .setId(SESSION_ID)
                    .setUserId(USER_ID);
            when(sessionMapper.selectById(SESSION_ID)).thenReturn(entity);

            service.clearMessages(SESSION_ID, USER_ID);

            verify(messageMapper).delete(any());
            verify(sessionMapper, never()).deleteById(any(java.io.Serializable.class));
        }
    }

    @Nested
    @DisplayName("switchAgent")
    class SwitchAgent {

        @Test
        @DisplayName("updates agent_id and inserts separator message")
        void switchesAgent() {
            UUID newAgentId = UUID.randomUUID();
            ChatSessionEntity entity = new ChatSessionEntity()
                    .setId(SESSION_ID)
                    .setUserId(USER_ID)
                    .setCurrentAgentId(AGENT_ID);
            when(sessionMapper.selectById(SESSION_ID)).thenReturn(entity);

            service.switchAgent(SESSION_ID, newAgentId, USER_ID);

            ArgumentCaptor<ChatSessionEntity> sessionCaptor = ArgumentCaptor.forClass(ChatSessionEntity.class);
            verify(sessionMapper).updateById(sessionCaptor.capture());
            assertThat(sessionCaptor.getValue().getCurrentAgentId()).isEqualTo(newAgentId);

            ArgumentCaptor<ChatMessageEntity> msgCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
            verify(messageMapper).insert(msgCaptor.capture());
            ChatMessageEntity separator = msgCaptor.getValue();
            assertThat(separator.getRole()).isEqualTo("separator");
            assertThat(separator.getSessionId()).isEqualTo(SESSION_ID);
        }
    }
}
