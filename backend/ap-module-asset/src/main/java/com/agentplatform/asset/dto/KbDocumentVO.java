package com.agentplatform.asset.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class KbDocumentVO {

    private UUID id;

    private UUID knowledgeBaseId;

    private UUID fileId;

    private String filename;

    private Long fileSize;

    private String mimeType;

    private String scanStatus;

    private String indexStatus;

    private String indexError;

    private Integer chunkCount;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
