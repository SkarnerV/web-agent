package com.agentplatform.chat.service;

import com.agentplatform.chat.converter.ChatConverter;
import com.agentplatform.chat.dto.*;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import com.agentplatform.chat.mapper.ChatMessageMapper;
import com.agentplatform.chat.mapper.ChatSessionMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatSessionService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatConverter chatConverter;

    public ChatSessionService(ChatSessionMapper sessionMapper,
                              ChatMessageMapper messageMapper,
                              ChatConverter chatConverter) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.chatConverter = chatConverter;
    }

    @Transactional
    public ChatSessionVO createSession(CreateSessionRequest request, UUID userId) {
        ChatSessionEntity entity = new ChatSessionEntity()
                .setUserId(userId)
                .setCurrentAgentId(request.getAgentId())
                .setTitle("新对话")
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        sessionMapper.insert(entity);
        return chatConverter.toSessionVO(entity);
    }

    public PageResult<ChatSessionVO> listSessions(UUID userId, int page, int pageSize) {
        LambdaQueryWrapper<ChatSessionEntity> wrapper = new LambdaQueryWrapper<ChatSessionEntity>()
                .eq(ChatSessionEntity::getUserId, userId)
                .orderByDesc(ChatSessionEntity::getUpdatedAt);

        IPage<ChatSessionEntity> result = sessionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ChatSessionVO> vos = result.getRecords().stream()
                .map(chatConverter::toSessionVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), page, pageSize);
    }

    public ChatSessionDetailVO getSessionDetail(UUID sessionId, UUID userId) {
        ChatSessionEntity session = getSessionOrThrow(sessionId, userId);

        List<ChatMessageEntity> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .orderByAsc(ChatMessageEntity::getCreatedAt));

        ChatSessionDetailVO detail = new ChatSessionDetailVO();
        detail.setId(session.getId());
        detail.setUserId(session.getUserId());
        detail.setCurrentAgentId(session.getCurrentAgentId());
        detail.setTitle(session.getTitle());
        detail.setCreatedAt(session.getCreatedAt());
        detail.setUpdatedAt(session.getUpdatedAt());
        detail.setMessages(messages.stream().map(chatConverter::toMessageVO).toList());
        return detail;
    }

    @Transactional
    public void clearMessages(UUID sessionId, UUID userId) {
        getSessionOrThrow(sessionId, userId);
        messageMapper.delete(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId));
    }

    @Transactional
    public void switchAgent(UUID sessionId, UUID newAgentId, UUID userId) {
        ChatSessionEntity session = getSessionOrThrow(sessionId, userId);
        session.setCurrentAgentId(newAgentId);
        session.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(session);

        ChatMessageEntity separator = new ChatMessageEntity()
                .setSessionId(sessionId)
                .setRole("separator")
                .setContent("— 已切换到新 Agent —")
                .setStatus("complete")
                .setCreatedAt(OffsetDateTime.now());
        messageMapper.insert(separator);
    }

    public ChatSessionEntity getSessionOrThrow(UUID sessionId, UUID userId) {
        ChatSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        return session;
    }

    public void updateSessionTitle(UUID sessionId, String title) {
        ChatSessionEntity session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setTitle(title);
            session.setUpdatedAt(OffsetDateTime.now());
            sessionMapper.updateById(session);
        }
    }
}
