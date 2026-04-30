package com.agentplatform.agent.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CustomModelVO {

    private UUID id;

    private String name;

    private String apiUrl;

    private String apiKeyMasked;

    private String connectionStatus;

    private String lastError;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
