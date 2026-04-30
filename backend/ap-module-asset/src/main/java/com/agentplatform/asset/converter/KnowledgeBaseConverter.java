package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.KbDocumentVO;
import com.agentplatform.asset.dto.KnowledgeBaseCreateRequest;
import com.agentplatform.asset.dto.KnowledgeBaseDetailVO;
import com.agentplatform.asset.dto.KnowledgeBaseSummaryVO;
import com.agentplatform.asset.entity.KbDocumentEntity;
import com.agentplatform.asset.entity.KnowledgeBaseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface KnowledgeBaseConverter {

    KnowledgeBaseDetailVO toDetailVO(KnowledgeBaseEntity entity);

    KnowledgeBaseSummaryVO toSummaryVO(KnowledgeBaseEntity entity);

    KnowledgeBaseEntity toEntity(KnowledgeBaseCreateRequest request);

    KbDocumentVO toDocumentVO(KbDocumentEntity entity);
}
