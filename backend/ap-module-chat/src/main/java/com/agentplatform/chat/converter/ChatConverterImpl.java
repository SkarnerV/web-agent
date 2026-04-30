package com.agentplatform.chat.converter;

import com.agentplatform.chat.dto.ChatMessageVO;
import com.agentplatform.chat.dto.ChatSessionVO;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatConverterImpl implements ChatConverter {

    @Override
    public ChatSessionVO toSessionVO(ChatSessionEntity entity) {
        if (entity == null) return null;
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setCurrentAgentId(entity.getCurrentAgentId());
        vo.setTitle(entity.getTitle());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @Override
    public ChatMessageVO toMessageVO(ChatMessageEntity entity) {
        if (entity == null) return null;
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setRole(entity.getRole());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setToolCalls(entity.getToolCalls());
        vo.setToolResults(entity.getToolResults());
        vo.setAttachments(entity.getAttachments());
        vo.setAgentId(entity.getAgentId());
        vo.setModelId(entity.getModelId());
        vo.setStepCount(entity.getStepCount());
        vo.setUsage(entity.getUsage());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
