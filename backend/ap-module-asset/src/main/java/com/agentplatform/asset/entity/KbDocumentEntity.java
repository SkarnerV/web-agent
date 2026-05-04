package com.agentplatform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Document record within a knowledge base. Does NOT extend BaseEntity because the kb_documents
 * table has no version/soft-delete columns — documents are hard-deleted via ON DELETE CASCADE
 * when the parent KB is removed, and concurrent edits are not a concern.
 */
@Data
@Accessors(chain = true)
@TableName("kb_documents")
public class KbDocumentEntity {

    @TableId(type = IdType.INPUT)
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
