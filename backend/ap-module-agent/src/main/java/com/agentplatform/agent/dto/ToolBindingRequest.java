package com.agentplatform.agent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ToolBindingRequest {

    private String sourceType;

    private UUID sourceId;

    private String toolName;

    private Boolean enabled = true;
}
