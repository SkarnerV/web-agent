package com.agentplatform.chat.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ChatMessageVO {

    private UUID id;

    private UUID sessionId;

    private String role;

    private String content;

    private String status;

    private String toolCalls;

    private String toolResults;

    private String attachments;

    private UUID agentId;

    private String modelId;

    private Integer stepCount;

    private String usage;

    private OffsetDateTime createdAt;
}
