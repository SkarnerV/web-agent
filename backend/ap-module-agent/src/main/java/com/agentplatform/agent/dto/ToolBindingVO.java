package com.agentplatform.agent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ToolBindingVO {

    private UUID id;

    private String sourceType;

    private UUID sourceId;

    private String toolName;

    private String toolSchemaSnapshot;

    private Boolean enabled;

    private Integer sortOrder;
}
