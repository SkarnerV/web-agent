package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.McpCreateRequest;
import com.agentplatform.asset.dto.McpDetailVO;
import com.agentplatform.asset.dto.McpSummaryVO;
import com.agentplatform.asset.entity.McpEntity;
import org.springframework.stereotype.Component;

@Component
public class McpConverterImpl implements McpConverter {

    @Override
    public McpDetailVO toDetailVO(McpEntity entity) {
        if (entity == null) return null;
        McpDetailVO vo = new McpDetailVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setName(entity.getName());
        vo.setUrl(entity.getUrl());
        vo.setProtocol(entity.getProtocol());
        vo.setJsonConfig(entity.getJsonConfig());
        vo.setEnabled(entity.getEnabled());
        vo.setConnectionStatus(entity.getConnectionStatus());
        vo.setLastError(entity.getLastError());
        vo.setToolsDiscovered(entity.getToolsDiscovered());
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
    public McpSummaryVO toSummaryVO(McpEntity entity) {
        if (entity == null) return null;
        McpSummaryVO vo = new McpSummaryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setUrl(entity.getUrl());
        vo.setProtocol(entity.getProtocol());
        vo.setEnabled(entity.getEnabled());
        vo.setConnectionStatus(entity.getConnectionStatus());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    @Override
    public McpEntity toEntity(McpCreateRequest request) {
        if (request == null) return null;
        McpEntity entity = new McpEntity();
        entity.setName(request.getName());
        entity.setUrl(request.getUrl());
        entity.setProtocol(request.getProtocol());
        entity.setJsonConfig(request.getJsonConfig());
        return entity;
    }
}
