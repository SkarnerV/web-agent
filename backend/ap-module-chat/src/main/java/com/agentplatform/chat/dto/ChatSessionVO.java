package com.agentplatform.chat.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ChatSessionVO {

    private UUID id;

    private UUID userId;

    private UUID currentAgentId;

    private String title;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
