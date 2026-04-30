package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class KnowledgeBaseSummaryVO {

    private UUID id;

    private String name;

    private String description;

    private String visibility;

    private Integer docCount;

    private Long totalSizeBytes;

    private UUID ownerId;

    private OffsetDateTime createdAt;
}
