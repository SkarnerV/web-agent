package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.KbDocumentVO;
import com.agentplatform.asset.dto.KnowledgeBaseCreateRequest;
import com.agentplatform.asset.dto.KnowledgeBaseDetailVO;
import com.agentplatform.asset.dto.KnowledgeBaseSummaryVO;
import com.agentplatform.asset.entity.KbDocumentEntity;
import com.agentplatform.asset.entity.KnowledgeBaseEntity;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseConverterImpl implements KnowledgeBaseConverter {

    @Override
    public KnowledgeBaseDetailVO toDetailVO(KnowledgeBaseEntity entity) {
        if (entity == null) return null;
        KnowledgeBaseDetailVO vo = new KnowledgeBaseDetailVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setIndexConfig(entity.getIndexConfig());
        vo.setVisibility(entity.getVisibility());
        vo.setDocCount(entity.getDocCount());
        vo.setTotalSizeBytes(entity.getTotalSizeBytes());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setVersion(entity.getVersion());
        return vo;
    }

    @Override
    public KnowledgeBaseSummaryVO toSummaryVO(KnowledgeBaseEntity entity) {
        if (entity == null) return null;
        KnowledgeBaseSummaryVO vo = new KnowledgeBaseSummaryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setVisibility(entity.getVisibility());
        vo.setDocCount(entity.getDocCount());
        vo.setTotalSizeBytes(entity.getTotalSizeBytes());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    @Override
    public KnowledgeBaseEntity toEntity(KnowledgeBaseCreateRequest request) {
        if (request == null) return null;
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setIndexConfig(request.getIndexConfig());
        return entity;
    }

    @Override
    public KbDocumentVO toDocumentVO(KbDocumentEntity entity) {
        if (entity == null) return null;
        KbDocumentVO vo = new KbDocumentVO();
        vo.setId(entity.getId());
        vo.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        vo.setFileId(entity.getFileId());
        vo.setFilename(entity.getFilename());
        vo.setFileSize(entity.getFileSize());
        vo.setMimeType(entity.getMimeType());
        vo.setScanStatus(entity.getScanStatus());
        vo.setIndexStatus(entity.getIndexStatus());
        vo.setIndexError(entity.getIndexError());
        vo.setChunkCount(entity.getChunkCount());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
