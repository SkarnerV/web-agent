package com.agentplatform.chat.converter;

import com.agentplatform.chat.dto.ChatMessageVO;
import com.agentplatform.chat.dto.ChatSessionVO;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;

public interface ChatConverter {

    ChatSessionVO toSessionVO(ChatSessionEntity entity);

    ChatMessageVO toMessageVO(ChatMessageEntity entity);
}
