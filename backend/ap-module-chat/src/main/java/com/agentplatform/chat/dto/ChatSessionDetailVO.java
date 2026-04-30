package com.agentplatform.chat.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatSessionDetailVO extends ChatSessionVO {

    private List<ChatMessageVO> messages;
}
