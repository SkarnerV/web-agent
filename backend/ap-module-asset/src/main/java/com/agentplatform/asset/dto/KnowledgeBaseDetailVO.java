package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class KnowledgeBaseDetailVO {

    private UUID id;

    private UUID ownerId;

    private String name;

    private String description;

    private String indexConfig;

    private String visibility;

    private Integer docCount;

    private Long totalSizeBytes;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long version;
}
