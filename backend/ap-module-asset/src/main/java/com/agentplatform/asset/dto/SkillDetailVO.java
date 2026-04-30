package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class SkillDetailVO {

    private UUID id;

    private UUID ownerId;

    private String name;

    private String description;

    private String triggerConditions;

    private String format;

    private String content;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long version;
}
