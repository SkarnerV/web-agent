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
@TableName("files")
public class FileEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private UUID ownerId;

    private UUID sessionId;

    private String source;

    private UUID generatedByMessageId;

    private String filename;

    private Long fileSize;

    private String mimeType;

    private String storagePath;

    private String storageType;

    private String scanStatus;

    private String scanError;

    private String status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;
}
