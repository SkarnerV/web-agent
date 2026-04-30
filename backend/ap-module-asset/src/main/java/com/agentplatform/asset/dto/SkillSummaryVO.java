package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class SkillSummaryVO {

    private UUID id;

    private String name;

    private String description;

    private String status;

    private String visibility;

    private UUID ownerId;

    private OffsetDateTime createdAt;
}
