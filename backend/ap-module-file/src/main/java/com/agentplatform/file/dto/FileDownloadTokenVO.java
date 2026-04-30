package com.agentplatform.file.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FileDownloadTokenVO {
    private String downloadUrl;
    private OffsetDateTime expiresAt;
}
