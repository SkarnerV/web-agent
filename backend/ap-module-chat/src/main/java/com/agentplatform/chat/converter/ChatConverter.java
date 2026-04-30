package com.agentplatform.chat.converter;

import com.agentplatform.chat.dto.ChatMessageVO;
import com.agentplatform.chat.dto.ChatSessionVO;
import com.agentplatform.chat.entity.ChatMessageEntity;
import com.agentplatform.chat.entity.ChatSessionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatConverter {

    ChatSessionVO toSessionVO(ChatSessionEntity entity);

    ChatMessageVO toMessageVO(ChatMessageEntity entity);
}
