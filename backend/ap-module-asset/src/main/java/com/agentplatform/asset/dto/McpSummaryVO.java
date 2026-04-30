package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class McpSummaryVO {

    private UUID id;

    private String name;

    private String url;

    private String protocol;

    private Boolean enabled;

    private String connectionStatus;

    private Integer toolsDiscoveredCount;

    private String status;

    private String visibility;

    private UUID ownerId;

    private OffsetDateTime createdAt;
}
