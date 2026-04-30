package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.SkillCreateRequest;
import com.agentplatform.asset.dto.SkillDetailVO;
import com.agentplatform.asset.dto.SkillSummaryVO;
import com.agentplatform.asset.entity.SkillEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SkillConverter {

    SkillDetailVO toDetailVO(SkillEntity entity);

    SkillSummaryVO toSummaryVO(SkillEntity entity);

    SkillEntity toEntity(SkillCreateRequest request);
}
