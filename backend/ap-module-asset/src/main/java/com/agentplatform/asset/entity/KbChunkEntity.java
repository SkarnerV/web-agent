package com.agentplatform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.agentplatform.common.mybatis.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "kb_chunks", autoResultMap = true)
public class KbChunkEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private UUID documentId;

    private UUID knowledgeBaseId;

    private Integer chunkIndex;

    private String content;

    // pgvector embedding field intentionally omitted; handled via custom SQL

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;

    private OffsetDateTime createdAt;
}
