package com.agentplatform.file.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FileVO {
    private UUID id;
    private String filename;
    private Long fileSize;
    private String mimeType;
    private String scanStatus;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
