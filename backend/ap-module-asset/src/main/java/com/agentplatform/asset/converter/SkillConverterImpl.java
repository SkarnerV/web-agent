package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.SkillCreateRequest;
import com.agentplatform.asset.dto.SkillDetailVO;
import com.agentplatform.asset.dto.SkillSummaryVO;
import com.agentplatform.asset.entity.SkillEntity;
import org.springframework.stereotype.Component;

@Component
public class SkillConverterImpl implements SkillConverter {

    @Override
    public SkillDetailVO toDetailVO(SkillEntity entity) {
        if (entity == null) return null;
        SkillDetailVO vo = new SkillDetailVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setTriggerConditions(entity.getTriggerConditions());
        vo.setFormat(entity.getFormat());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setHasUnpublishedChanges(entity.getHasUnpublishedChanges());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setVersion(entity.getVersion());
        return vo;
    }

    @Override
    public SkillSummaryVO toSummaryVO(SkillEntity entity) {
        if (entity == null) return null;
        SkillSummaryVO vo = new SkillSummaryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    @Override
    public SkillEntity toEntity(SkillCreateRequest request) {
        if (request == null) return null;
        SkillEntity entity = new SkillEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setTriggerConditions(request.getTriggerConditions());
        entity.setFormat(request.getFormat());
        entity.setContent(request.getContent());
        return entity;
    }
}
