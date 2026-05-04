package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class McpDetailVO {

    private UUID id;

    private UUID ownerId;

    private String name;

    private String description;

    private String url;

    private String protocol;

    private String authHeadersMasked;

    private String jsonConfig;

    private Boolean enabled;

    private String connectionStatus;

    private String lastError;

    private String toolsDiscovered;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long version;
}
