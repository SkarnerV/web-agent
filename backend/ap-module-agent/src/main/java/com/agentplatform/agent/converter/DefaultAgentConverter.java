package com.agentplatform.agent.converter;

import com.agentplatform.agent.dto.AgentCreateRequest;
import com.agentplatform.agent.dto.AgentDetailVO;
import com.agentplatform.agent.dto.AgentSummaryVO;
import com.agentplatform.agent.dto.AssetVersionVO;
import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.agent.dto.ToolBindingVO;
import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.entity.BuiltinModelEntity;
import com.agentplatform.agent.entity.CustomModelEntity;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DefaultAgentConverter implements AgentConverter {

    @Override
    public AgentDetailVO toDetailVO(AgentEntity entity) {
        if (entity == null) {
            return null;
        }
        AgentDetailVO vo = new AgentDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setAvatar(entity.getAvatar());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setMaxSteps(entity.getMaxSteps());
        vo.setModelId(entity.getModelId());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setHasUnpublishedChanges(entity.getHasUnpublishedChanges());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setVersion(entity.getVersion());
        return vo;
    }

    @Override
    public AgentSummaryVO toSummaryVO(AgentEntity entity) {
        if (entity == null) {
            return null;
        }
        AgentSummaryVO vo = new AgentSummaryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setAvatar(entity.getAvatar());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setHasUnpublishedChanges(entity.getHasUnpublishedChanges());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @Override
    public AgentEntity toEntity(AgentCreateRequest request) {
        if (request == null) {
            return null;
        }
        AgentEntity entity = new AgentEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAvatar(request.getAvatar());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setMaxSteps(request.getMaxSteps());
        entity.setModelId(request.getModelId());
        return entity;
    }

    @Override
    public ToolBindingVO toToolBindingVO(AgentToolBindingEntity entity) {
        if (entity == null) {
            return null;
        }
        ToolBindingVO vo = new ToolBindingVO();
        vo.setId(entity.getId());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceId(entity.getSourceId());
        vo.setToolName(entity.getToolName());
        vo.setToolSchemaSnapshot(entity.getToolSchemaSnapshot());
        vo.setEnabled(entity.getEnabled());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    @Override
    public AssetVersionVO toAssetVersionVO(AssetVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        AssetVersionVO vo = new AssetVersionVO();
        vo.setId(entity.getId());
        vo.setAssetType(entity.getAssetType());
        vo.setAssetId(entity.getAssetId());
        vo.setVersion(entity.getVersion());
        vo.setReleaseNotes(entity.getReleaseNotes());
        vo.setPublishedBy(entity.getPublishedBy());
        vo.setPublishedAt(entity.getPublishedAt());
        return vo;
    }

    @Override
    public BuiltinModelVO toBuiltinModelVO(BuiltinModelEntity entity) {
        if (entity == null) {
            return null;
        }
        BuiltinModelVO vo = new BuiltinModelVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setProvider(entity.getProvider());
        vo.setDescription(entity.getDescription());
        vo.setIsDefault(entity.getIsDefault());
        vo.setEnabled(entity.getEnabled());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    @Override
    public CustomModelVO toCustomModelVO(CustomModelEntity entity) {
        if (entity == null) {
            return null;
        }
        CustomModelVO vo = new CustomModelVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setApiUrl(entity.getApiUrl());
        vo.setConnectionStatus(entity.getConnectionStatus());
        vo.setLastError(entity.getLastError());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
