package com.agentplatform.agent.converter;

import com.agentplatform.agent.dto.AgentDetailVO;
import com.agentplatform.agent.dto.AgentSummaryVO;
import com.agentplatform.agent.dto.AssetVersionVO;
import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.agent.dto.ToolBindingVO;
import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AgentToolBindingEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.entity.BuiltinModelEntity;
import com.agentplatform.agent.entity.CustomModelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AgentConverter {

    @Mapping(target = "toolBindings", ignore = true)
    @Mapping(target = "skillIds", ignore = true)
    @Mapping(target = "knowledgeBaseIds", ignore = true)
    AgentDetailVO toDetailVO(AgentEntity entity);

    AgentSummaryVO toSummaryVO(AgentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "visibility", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "hasUnpublishedChanges", ignore = true)
    AgentEntity toEntity(com.agentplatform.agent.dto.AgentCreateRequest request);

    ToolBindingVO toToolBindingVO(AgentToolBindingEntity entity);

    AssetVersionVO toAssetVersionVO(AssetVersionEntity entity);

    BuiltinModelVO toBuiltinModelVO(BuiltinModelEntity entity);

    @Mapping(target = "apiKeyMasked", ignore = true)
    CustomModelVO toCustomModelVO(CustomModelEntity entity);
}
