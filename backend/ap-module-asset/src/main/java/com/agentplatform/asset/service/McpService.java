package com.agentplatform.asset.service;

import com.agentplatform.asset.client.McpClient;
import com.agentplatform.asset.converter.McpConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.enums.AssetStatus;
import com.agentplatform.common.core.enums.ConnectionStatus;
import com.agentplatform.common.core.enums.McpProtocol;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.*;
import com.agentplatform.common.core.tool.ToolRegistry;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final McpMapper mcpMapper;
    private final McpConverter mcpConverter;
    private final AgentToolBindingMapper agentToolBindingMapper;
    private final CredentialStore credentialStore;
    private final ToolRegistry toolRegistry;
    private final PermissionChecker permissionChecker;
    private final ObjectMapper objectMapper;
    private final McpClient mcpClient;

    public McpService(McpMapper mcpMapper,
                      McpConverter mcpConverter,
                      AgentToolBindingMapper agentToolBindingMapper,
                      CredentialStore credentialStore,
                      ToolRegistry toolRegistry,
                      PermissionChecker permissionChecker,
                      ObjectMapper objectMapper,
                      McpClient mcpClient) {
        this.mcpMapper = mcpMapper;
        this.mcpConverter = mcpConverter;
        this.agentToolBindingMapper = agentToolBindingMapper;
        this.credentialStore = credentialStore;
        this.toolRegistry = toolRegistry;
        this.permissionChecker = permissionChecker;
        this.objectMapper = objectMapper;
        this.mcpClient = mcpClient;
    }

    @Transactional
    public McpDetailVO create(McpCreateRequest request, UUID currentUserId) {
        validateProtocol(request.getProtocol());

        McpEntity entity = mcpConverter.toEntity(request);
        entity.setOwnerId(currentUserId);
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setEnabled(true);
        entity.setConnectionStatus(ConnectionStatus.OFFLINE.getValue());
        entity.setCurrentVersion("v1.0.0");
        entity.setHasUnpublishedChanges(false);
        if (request.getAuthHeaders() != null && !request.getAuthHeaders().isBlank()) {
            entity.setAuthHeadersEnc(credentialStore.encrypt(request.getAuthHeaders()).getBytes());
        }
        mcpMapper.insert(entity);

        return getDetail(entity.getId(), currentUserId);
    }

    public PageResult<McpSummaryVO> list(UUID currentUserId, String search,
                                          int page, int pageSize, String sortBy, String sortOrder) {
        LambdaQueryWrapper<McpEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpEntity::getOwnerId, currentUserId);
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(McpEntity::getName, search)
                    .or().like(McpEntity::getUrl, search));
        }
        String column = "created_at".equals(sortBy) ? "created_at" : "updated_at";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column.equals("created_at") ? McpEntity::getCreatedAt : McpEntity::getUpdatedAt);
        } else {
            wrapper.orderByDesc(column.equals("created_at") ? McpEntity::getCreatedAt : McpEntity::getUpdatedAt);
        }

        IPage<McpEntity> pageResult = mcpMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<McpSummaryVO> voList = pageResult.getRecords().stream()
                .map(e -> {
                    McpSummaryVO vo = mcpConverter.toSummaryVO(e);
                    vo.setToolsDiscoveredCount(countTools(e.getToolsDiscovered()));
                    return vo;
                })
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    public McpDetailVO getDetail(UUID mcpId, UUID currentUserId) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.READ);

        McpDetailVO vo = mcpConverter.toDetailVO(entity);
        maskAuthHeaders(vo, entity);
        return vo;
    }

    @Transactional
    public McpDetailVO update(UUID mcpId, McpUpdateRequest request, UUID currentUserId) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.WRITE);

        if (request.getVersion() != null && !request.getVersion().equals(entity.getVersion())) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getUrl() != null) entity.setUrl(request.getUrl());
        if (request.getProtocol() != null) {
            validateProtocol(request.getProtocol());
            entity.setProtocol(request.getProtocol());
        }
        if (request.getJsonConfig() != null) entity.setJsonConfig(request.getJsonConfig());
        if (request.getAuthHeaders() != null) {
            entity.setAuthHeadersEnc(credentialStore.encrypt(request.getAuthHeaders()).getBytes());
        }

        if (AssetStatus.PUBLISHED.getValue().equals(entity.getStatus())) {
            entity.setHasUnpublishedChanges(true);
        }

        int rows = mcpMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        toolRegistry.refreshMcpTools(mcpId);
        return getDetail(mcpId, currentUserId);
    }

    @Transactional
    public void delete(UUID mcpId, UUID currentUserId, boolean force) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.DELETE);

        List<AgentToolBindingEntity> dependents = agentToolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getSourceType, "mcp")
                        .eq(AgentToolBindingEntity::getSourceId, mcpId));

        if (!dependents.isEmpty() && !force) {
            List<UUID> referrerAgentIds = dependents.stream()
                    .map(AgentToolBindingEntity::getAgentId)
                    .distinct()
                    .toList();
            throw new BizException(ErrorCode.ASSET_DELETE_CONFLICT,
                    Map.of("referrer_agent_ids", referrerAgentIds, "count", referrerAgentIds.size()));
        }

        if (!dependents.isEmpty()) {
            agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                    .eq(AgentToolBindingEntity::getSourceType, "mcp")
                    .eq(AgentToolBindingEntity::getSourceId, mcpId));
        }

        toolRegistry.refreshMcpTools(mcpId);
        mcpMapper.deleteById(mcpId);
        log.info("Deleted MCP {} (force={})", mcpId, force);
    }

    @Transactional
    public McpDetailVO toggle(UUID mcpId, UUID currentUserId, boolean enabled) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.WRITE);

        entity.setEnabled(enabled);
        if (!enabled) {
            entity.setConnectionStatus(ConnectionStatus.OFFLINE.getValue());
        }
        mcpMapper.updateById(entity);

        toolRegistry.refreshMcpTools(mcpId);
        return getDetail(mcpId, currentUserId);
    }

    public Map<String, Object> export(UUID mcpId, UUID currentUserId) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.READ);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", entity.getName());
        data.put("description", entity.getDescription());
        data.put("url", entity.getUrl());
        data.put("protocol", entity.getProtocol());
        data.put("json_config", entity.getJsonConfig());
        data.put("tools_discovered", parseToolsDiscovered(entity.getToolsDiscovered()));
        return data;
    }

    @Transactional
    public McpDetailVO testConnection(UUID mcpId, UUID currentUserId) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.READ);

        try {
            Map<String, Object> result = mcpClient.testConnection(
                    entity.getUrl(), entity.getProtocol(), decryptAuth(entity));
            entity.setConnectionStatus(ConnectionStatus.CONNECTED.getValue());
            entity.setLastError(null);
            mcpMapper.updateById(entity);
            log.info("MCP {} connection test successful: {}", mcpId, result);
        } catch (Exception e) {
            entity.setConnectionStatus(ConnectionStatus.FAILED.getValue());
            entity.setLastError(e.getMessage());
            mcpMapper.updateById(entity);
            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                    Map.of("reason", e.getMessage()));
        }

        return getDetail(mcpId, currentUserId);
    }

    @Transactional
    public McpDetailVO discoverTools(UUID mcpId, UUID currentUserId) {
        McpEntity entity = getEntityOrThrow(mcpId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.WRITE);

        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                    Map.of("reason", "MCP is disabled"));
        }

        try {
            List<Map<String, Object>> tools = mcpClient.discoverTools(
                    entity.getUrl(), entity.getProtocol(), decryptAuth(entity));
            entity.setToolsDiscovered(toJson(tools));
            entity.setConnectionStatus(ConnectionStatus.CONNECTED.getValue());
            entity.setLastError(null);
            mcpMapper.updateById(entity);

            toolRegistry.refreshMcpTools(mcpId);
            log.info("MCP {} tool discovery complete: {} tools found", mcpId, tools.size());
        } catch (Exception e) {
            entity.setConnectionStatus(ConnectionStatus.FAILED.getValue());
            entity.setLastError(e.getMessage());
            mcpMapper.updateById(entity);
            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                    Map.of("reason", e.getMessage()));
        }

        return getDetail(mcpId, currentUserId);
    }

    // ─── helpers ───

    private McpEntity getEntityOrThrow(UUID mcpId) {
        McpEntity entity = mcpMapper.selectById(mcpId);
        if (entity == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        return entity;
    }

    private AssetRef toAssetRef(McpEntity entity) {
        AssetVisibility visibility;
        try {
            visibility = AssetVisibility.valueOf(entity.getVisibility().toUpperCase());
        } catch (IllegalArgumentException e) {
            visibility = AssetVisibility.PRIVATE;
        }
        return new AssetRef(entity.getId(), entity.getOwnerId(), visibility, entity.getDeletedAt());
    }

    private void validateProtocol(String protocol) {
        try {
            McpProtocol.valueOf(protocol.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.MCP_PROTOCOL_INVALID,
                    Map.of("protocol", protocol));
        }
    }

    private void maskAuthHeaders(McpDetailVO vo, McpEntity entity) {
        if (entity.getAuthHeadersEnc() != null && entity.getAuthHeadersEnc().length > 0) {
            String stored = new String(entity.getAuthHeadersEnc());
            vo.setAuthHeadersMasked(credentialStore.mask(stored));
        }
    }

    private String decryptAuth(McpEntity entity) {
        if (entity.getAuthHeadersEnc() != null && entity.getAuthHeadersEnc().length > 0) {
            return credentialStore.decrypt(new String(entity.getAuthHeadersEnc()));
        }
        return null;
    }

    private int countTools(String toolsDiscovered) {
        List<?> tools = parseToolsDiscovered(toolsDiscovered);
        return tools != null ? tools.size() : 0;
    }

    private List<?> parseToolsDiscovered(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
