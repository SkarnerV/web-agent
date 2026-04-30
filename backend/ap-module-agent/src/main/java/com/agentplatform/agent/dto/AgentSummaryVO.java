package com.agentplatform.agent.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AgentSummaryVO {

    private UUID id;

    private String name;

    private String description;

    private String avatar;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;

    private UUID ownerId;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
