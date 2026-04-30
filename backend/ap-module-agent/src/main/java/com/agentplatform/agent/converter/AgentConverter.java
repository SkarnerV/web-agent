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
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AgentConverter {

    AgentDetailVO toDetailVO(AgentEntity entity);

    AgentSummaryVO toSummaryVO(AgentEntity entity);

    AgentEntity toEntity(com.agentplatform.agent.dto.AgentCreateRequest request);

    ToolBindingVO toToolBindingVO(AgentToolBindingEntity entity);

    AssetVersionVO toAssetVersionVO(AssetVersionEntity entity);

    BuiltinModelVO toBuiltinModelVO(BuiltinModelEntity entity);

    CustomModelVO toCustomModelVO(CustomModelEntity entity);
}
