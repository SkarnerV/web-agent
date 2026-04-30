package com.agentplatform.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("file_download_tokens")
public class FileDownloadTokenEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID fileId;

    private UUID userId;

    private UUID sessionId;

    private String token;

    private String tokenType;

    private Boolean used;

    private OffsetDateTime expiresAt;

    private OffsetDateTime usedAt;

    private String userAgent;

    private String ipAddress;

    private OffsetDateTime createdAt;
}
