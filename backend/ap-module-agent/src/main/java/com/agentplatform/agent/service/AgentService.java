package com.agentplatform.agent.service;

import com.agentplatform.agent.converter.AgentConverter;
import com.agentplatform.agent.dto.*;
import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AgentToolBindingEntity;
import com.agentplatform.agent.entity.AssetReferenceEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AgentToolBindingMapper;
import com.agentplatform.agent.mapper.AssetReferenceMapper;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.common.core.agent.SourceResolver;
import com.agentplatform.common.core.enums.AssetStatus;
import com.agentplatform.common.core.enums.AssetType;
import com.agentplatform.common.core.enums.SourceType;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentMapper agentMapper;
    private final AgentToolBindingMapper toolBindingMapper;
    private final AssetReferenceMapper assetReferenceMapper;
    private final AssetVersionMapper assetVersionMapper;
    private final AgentConverter agentConverter;
    private final PermissionChecker permissionChecker;
    private final ObjectMapper objectMapper;
    private final SourceResolver sourceResolver;

    public AgentService(AgentMapper agentMapper,
                        AgentToolBindingMapper toolBindingMapper,
                        AssetReferenceMapper assetReferenceMapper,
                        AssetVersionMapper assetVersionMapper,
                        AgentConverter agentConverter,
                        PermissionChecker permissionChecker,
                        ObjectMapper objectMapper,
                        SourceResolver sourceResolver) {
        this.agentMapper = agentMapper;
        this.toolBindingMapper = toolBindingMapper;
        this.assetReferenceMapper = assetReferenceMapper;
        this.assetVersionMapper = assetVersionMapper;
        this.agentConverter = agentConverter;
        this.permissionChecker = permissionChecker;
        this.objectMapper = objectMapper;
        this.sourceResolver = sourceResolver;
    }

    // ───────── 3.1 CRUD ─────────

    @Transactional
    public AgentDetailVO create(AgentCreateRequest request, UUID currentUserId) {
        AgentEntity entity = agentConverter.toEntity(request);
        entity.setOwnerId(currentUserId);
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setHasUnpublishedChanges(false);
        agentMapper.insert(entity);

        saveToolBindings(entity.getId(), request.getToolBindings());
        saveAssetReferences(entity.getId(), request.getSkillIds(), request.getKnowledgeBaseIds(), request.getCollaboratorAgentIds());

        return getDetail(entity.getId(), currentUserId);
    }

    public PageResult<AgentSummaryVO> list(UUID currentUserId, String status, String search,
                                           int page, int pageSize, String sortBy, String sortOrder) {
        LambdaQueryWrapper<AgentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentEntity::getOwnerId, currentUserId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(AgentEntity::getStatus, status);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(AgentEntity::getName, search)
                    .or().like(AgentEntity::getDescription, search));
        }

        String column = resolveColumn(sortBy);
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column.equals("updated_at") ? AgentEntity::getUpdatedAt : AgentEntity::getCreatedAt);
        } else {
            wrapper.orderByDesc(column.equals("updated_at") ? AgentEntity::getUpdatedAt : AgentEntity::getCreatedAt);
        }

        IPage<AgentEntity> pageResult = agentMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<AgentSummaryVO> voList = pageResult.getRecords().stream()
                .map(agentConverter::toSummaryVO)
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    public AgentDetailVO getDetail(UUID agentId, UUID currentUserId) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);

        AgentDetailVO vo = agentConverter.toDetailVO(entity);
        vo.setVersion(entity.getVersion());

        List<AgentToolBindingEntity> bindings = toolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId)
                        .orderByAsc(AgentToolBindingEntity::getSortOrder));
        vo.setToolBindings(bindings.stream().map(agentConverter::toToolBindingVO).toList());

        List<AssetReferenceEntity> refs = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                        .eq(AssetReferenceEntity::getReferrerId, agentId));
        vo.setSkillIds(filterRefIds(refs, "skill"));
        vo.setKnowledgeBaseIds(filterRefIds(refs, "knowledge"));

        return vo;
    }

    @Transactional
    public AgentDetailVO update(UUID agentId, AgentUpdateRequest request, UUID currentUserId) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.WRITE);

        if (request.getVersion() != null && !request.getVersion().equals(entity.getVersion())) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getAvatar() != null) entity.setAvatar(request.getAvatar());
        if (request.getSystemPrompt() != null) entity.setSystemPrompt(request.getSystemPrompt());
        if (request.getMaxSteps() != null) entity.setMaxSteps(request.getMaxSteps());
        if (request.getModelId() != null) entity.setModelId(request.getModelId());

        if (AssetStatus.PUBLISHED.getValue().equals(entity.getStatus())) {
            entity.setHasUnpublishedChanges(true);
        }

        int rows = agentMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        if (request.getToolBindings() != null) {
            toolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                    .eq(AgentToolBindingEntity::getAgentId, agentId));
            saveToolBindings(agentId, request.getToolBindings());
        }
        if (request.getSkillIds() != null || request.getKnowledgeBaseIds() != null || request.getCollaboratorAgentIds() != null) {
            assetReferenceMapper.delete(new LambdaQueryWrapper<AssetReferenceEntity>()
                    .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                    .eq(AssetReferenceEntity::getReferrerId, agentId));
            saveAssetReferences(agentId,
                    request.getSkillIds(),
                    request.getKnowledgeBaseIds(),
                    request.getCollaboratorAgentIds());
        }

        return getDetail(agentId, currentUserId);
    }

    @Transactional
    public void delete(UUID agentId, UUID currentUserId, boolean force) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.DELETE);

        List<AssetReferenceEntity> dependents = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getRefereeType, AssetType.AGENT.getValue())
                        .eq(AssetReferenceEntity::getRefereeId, agentId));

        if (!dependents.isEmpty() && !force) {
            List<UUID> referrerIds = dependents.stream()
                    .map(AssetReferenceEntity::getReferrerId)
                    .distinct()
                    .toList();
            throw new BizException(ErrorCode.ASSET_DELETE_CONFLICT,
                    Map.of("referrer_ids", referrerIds, "count", referrerIds.size()));
        }

        if (!dependents.isEmpty()) {
            assetReferenceMapper.delete(new LambdaQueryWrapper<AssetReferenceEntity>()
                    .eq(AssetReferenceEntity::getRefereeType, AssetType.AGENT.getValue())
                    .eq(AssetReferenceEntity::getRefereeId, agentId));
        }

        toolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                .eq(AgentToolBindingEntity::getAgentId, agentId));
        assetReferenceMapper.delete(new LambdaQueryWrapper<AssetReferenceEntity>()
                .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                .eq(AssetReferenceEntity::getReferrerId, agentId));

        agentMapper.deleteById(agentId);
    }

    // ───────── 3.2 Duplicate ─────────

    @Transactional
    public AgentDetailVO duplicate(UUID agentId, UUID currentUserId) {
        AgentEntity source = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(source);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);

        AgentEntity copy = new AgentEntity();
        copy.setOwnerId(currentUserId);
        copy.setName(source.getName() + "-Copy");
        copy.setDescription(source.getDescription());
        copy.setAvatar(source.getAvatar());
        copy.setSystemPrompt(source.getSystemPrompt());
        copy.setMaxSteps(source.getMaxSteps());
        copy.setModelId(source.getModelId());
        copy.setStatus(AssetStatus.DRAFT.getValue());
        copy.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        copy.setHasUnpublishedChanges(false);
        agentMapper.insert(copy);

        List<AgentToolBindingEntity> srcBindings = toolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId));
        for (AgentToolBindingEntity b : srcBindings) {
            AgentToolBindingEntity nb = new AgentToolBindingEntity();
            nb.setAgentId(copy.getId());
            nb.setSourceType(b.getSourceType());
            nb.setSourceId(b.getSourceId());
            nb.setToolName(b.getToolName());
            nb.setToolSchemaSnapshot(b.getToolSchemaSnapshot());
            nb.setEnabled(b.getEnabled());
            nb.setSortOrder(b.getSortOrder());
            nb.setCreatedAt(OffsetDateTime.now());
            nb.setUpdatedAt(OffsetDateTime.now());
            toolBindingMapper.insert(nb);
        }

        List<AssetReferenceEntity> srcRefs = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                        .eq(AssetReferenceEntity::getReferrerId, agentId));
        for (AssetReferenceEntity r : srcRefs) {
            AssetReferenceEntity nr = new AssetReferenceEntity();
            nr.setReferrerType(r.getReferrerType());
            nr.setReferrerId(copy.getId());
            nr.setRefereeType(r.getRefereeType());
            nr.setRefereeId(r.getRefereeId());
            nr.setRefKind(r.getRefKind());
            nr.setConfigParams(r.getConfigParams());
            assetReferenceMapper.insert(nr);
        }

        return getDetail(copy.getId(), currentUserId);
    }

    // ───────── 3.3 Export / Import ─────────

    public Map<String, Object> export(UUID agentId, UUID currentUserId) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);

        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("name", entity.getName());
        exportData.put("description", entity.getDescription());
        exportData.put("avatar", entity.getAvatar());
        exportData.put("system_prompt", entity.getSystemPrompt());
        exportData.put("max_steps", entity.getMaxSteps());
        exportData.put("model_id", entity.getModelId());

        List<AgentToolBindingEntity> bindings = toolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId)
                        .orderByAsc(AgentToolBindingEntity::getSortOrder));

        List<Map<String, Object>> toolBindingsExport = bindings.stream().map(b -> {
            Map<String, Object> tb = new LinkedHashMap<>();
            tb.put("source_type", b.getSourceType());
            tb.put("source_id", b.getSourceId());
            tb.put("source_name", sourceResolver.resolveSourceName(b.getSourceType(), b.getSourceId()));
            tb.put("source_url", sourceResolver.resolveSourceUrl(b.getSourceType(), b.getSourceId()));
            tb.put("tool_name", b.getToolName());
            tb.put("tool_schema_snapshot", parseJson(b.getToolSchemaSnapshot()));
            tb.put("enabled", b.getEnabled());
            return tb;
        }).toList();
        exportData.put("tool_bindings", toolBindingsExport);

        List<AssetReferenceEntity> refs = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                        .eq(AssetReferenceEntity::getReferrerId, agentId));
        exportData.put("skill_ids", filterRefIds(refs, "skill"));
        exportData.put("knowledge_base_ids", filterRefIds(refs, "knowledge"));
        exportData.put("collaborator_agent_ids", filterRefIds(refs, "collaborator"));

        return exportData;
    }

    @Transactional
    public AgentImportResult importAgent(String jsonContent, UUID currentUserId) {
        Map<String, Object> data;
        try {
            data = objectMapper.readValue(jsonContent, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.AGENT_IMPORT_INVALID, "导入文件 JSON 格式无效");
        }

        String name = (String) data.get("name");
        if (name == null || name.isBlank()) {
            throw new BizException(ErrorCode.AGENT_IMPORT_INVALID, "导入文件缺少 name 字段");
        }

        AgentEntity entity = new AgentEntity();
        entity.setOwnerId(currentUserId);
        entity.setName(name);
        entity.setDescription((String) data.get("description"));
        entity.setAvatar((String) data.get("avatar"));
        entity.setSystemPrompt((String) data.get("system_prompt"));
        entity.setMaxSteps(data.get("max_steps") != null ? ((Number) data.get("max_steps")).intValue() : 10);
        entity.setModelId((String) data.get("model_id"));
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setHasUnpublishedChanges(false);
        agentMapper.insert(entity);

        List<Map<String, Object>> unresolvedRefs = new ArrayList<>();
        Object tbRaw = data.get("tool_bindings");
        if (tbRaw instanceof List<?> tbList) {
            int sortIdx = 0;
            for (Object item : tbList) {
                if (!(item instanceof Map<?, ?> tbMap)) continue;

                String sourceType = (String) tbMap.get("source_type");
                Object sourceIdRaw = tbMap.get("source_id");
                String toolName = (String) tbMap.get("tool_name");

                UUID resolvedSourceId = resolveImportSourceId(sourceType, sourceIdRaw, tbMap);
                if (resolvedSourceId == null && !SourceType.BUILTIN.getValue().equals(sourceType)) {
                    Map<String, Object> unresolved = new LinkedHashMap<>();
                    unresolved.put("tool_name", toolName);
                    unresolved.put("source_type", sourceType);
                    unresolved.put("reason", "无法匹配工具来源");
                    unresolvedRefs.add(unresolved);
                    continue;
                }

                AgentToolBindingEntity binding = new AgentToolBindingEntity();
                binding.setAgentId(entity.getId());
                binding.setSourceType(sourceType);
                binding.setSourceId(resolvedSourceId);
                binding.setToolName(toolName);
                binding.setToolSchemaSnapshot(toJsonString(tbMap.get("tool_schema_snapshot")));
                binding.setEnabled(tbMap.get("enabled") != null ? (Boolean) tbMap.get("enabled") : true);
                binding.setSortOrder(sortIdx++);
                binding.setCreatedAt(OffsetDateTime.now());
                binding.setUpdatedAt(OffsetDateTime.now());
                toolBindingMapper.insert(binding);
            }
        }

        importAssetReferences(entity.getId(), data, "skill_ids", "skill", "skill");
        importAssetReferences(entity.getId(), data, "knowledge_base_ids", "knowledge_base", "knowledge");
        importAssetReferences(entity.getId(), data, "collaborator_agent_ids", "agent", "collaborator");

        AgentDetailVO detail = getDetail(entity.getId(), currentUserId);
        AgentImportResult result = new AgentImportResult();
        result.setAgent(detail);
        result.setUnresolvedRefs(unresolvedRefs);
        return result;
    }

    // ───────── 3.4 Version Management ─────────

    public List<AssetVersionVO> listVersions(UUID agentId, UUID currentUserId) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);

        List<AssetVersionEntity> versions = assetVersionMapper.selectList(
                new LambdaQueryWrapper<AssetVersionEntity>()
                        .eq(AssetVersionEntity::getAssetType, AssetType.AGENT.getValue())
                        .eq(AssetVersionEntity::getAssetId, agentId)
                        .orderByDesc(AssetVersionEntity::getPublishedAt));
        return versions.stream().map(agentConverter::toAssetVersionVO).toList();
    }

    @Transactional
    public AgentDetailVO rollback(UUID agentId, UUID versionId, UUID currentUserId) {
        AgentEntity entity = getEntityOrThrow(agentId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.WRITE);

        AssetVersionEntity version = assetVersionMapper.selectById(versionId);
        if (version == null ||
                !AssetType.AGENT.getValue().equals(version.getAssetType()) ||
                !agentId.equals(version.getAssetId())) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "指定的版本不存在");
        }

        Map<String, Object> snapshot;
        try {
            snapshot = objectMapper.readValue(version.getConfigSnapshot(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "版本快照解析失败");
        }

        if (snapshot.get("name") != null) entity.setName((String) snapshot.get("name"));
        if (snapshot.get("description") != null) entity.setDescription((String) snapshot.get("description"));
        if (snapshot.get("avatar") != null) entity.setAvatar((String) snapshot.get("avatar"));
        if (snapshot.get("system_prompt") != null) entity.setSystemPrompt((String) snapshot.get("system_prompt"));
        if (snapshot.get("max_steps") != null) entity.setMaxSteps(((Number) snapshot.get("max_steps")).intValue());
        if (snapshot.get("model_id") != null) entity.setModelId((String) snapshot.get("model_id"));
        entity.setHasUnpublishedChanges(true);

        int rows = agentMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        toolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                .eq(AgentToolBindingEntity::getAgentId, agentId));
        assetReferenceMapper.delete(new LambdaQueryWrapper<AssetReferenceEntity>()
                .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                .eq(AssetReferenceEntity::getReferrerId, agentId));

        Object tbRaw = snapshot.get("tool_bindings");
        if (tbRaw instanceof List<?> tbList) {
            int sortIdx = 0;
            for (Object item : tbList) {
                if (!(item instanceof Map<?, ?> tbMap)) continue;
                AgentToolBindingEntity binding = new AgentToolBindingEntity();
                binding.setAgentId(agentId);
                binding.setSourceType((String) tbMap.get("source_type"));
                binding.setSourceId(tbMap.get("source_id") != null ? UUID.fromString(tbMap.get("source_id").toString()) : null);
                binding.setToolName((String) tbMap.get("tool_name"));
                binding.setToolSchemaSnapshot(toJsonString(tbMap.get("tool_schema_snapshot")));
                binding.setEnabled(tbMap.get("enabled") != null ? (Boolean) tbMap.get("enabled") : true);
                binding.setSortOrder(sortIdx++);
                binding.setCreatedAt(OffsetDateTime.now());
                binding.setUpdatedAt(OffsetDateTime.now());
                toolBindingMapper.insert(binding);
            }
        }

        importAssetReferences(agentId, snapshot, "skill_ids", "skill", "skill");
        importAssetReferences(agentId, snapshot, "knowledge_base_ids", "knowledge_base", "knowledge");
        importAssetReferences(agentId, snapshot, "collaborator_agent_ids", "agent", "collaborator");

        return getDetail(agentId, currentUserId);
    }

    // ───────── Internal helpers ─────────

    private AgentEntity getEntityOrThrow(UUID agentId) {
        AgentEntity entity = agentMapper.selectById(agentId);
        if (entity == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        return entity;
    }

    private AssetRef toAssetRef(AgentEntity entity) {
        AssetVisibility visibility;
        try {
            visibility = AssetVisibility.valueOf(entity.getVisibility().toUpperCase());
        } catch (IllegalArgumentException e) {
            visibility = AssetVisibility.PRIVATE;
        }
        return new AssetRef(entity.getId(), entity.getOwnerId(), visibility, entity.getDeletedAt());
    }

    private void saveToolBindings(UUID agentId, List<ToolBindingRequest> bindings) {
        if (bindings == null) return;
        int sortIdx = 0;
        for (ToolBindingRequest tb : bindings) {
            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setAgentId(agentId);
            binding.setSourceType(tb.getSourceType());
            binding.setSourceId(tb.getSourceId());
            binding.setToolName(tb.getToolName());
            binding.setEnabled(tb.getEnabled() != null ? tb.getEnabled() : true);
            binding.setSortOrder(sortIdx++);
            binding.setCreatedAt(OffsetDateTime.now());
            binding.setUpdatedAt(OffsetDateTime.now());
            toolBindingMapper.insert(binding);
        }
    }

    private void saveAssetReferences(UUID agentId, List<UUID> skillIds,
                                     List<UUID> knowledgeBaseIds, List<UUID> collaboratorAgentIds) {
        insertRefs(agentId, skillIds, "skill", "skill");
        insertRefs(agentId, knowledgeBaseIds, "knowledge_base", "knowledge");
        insertRefs(agentId, collaboratorAgentIds, "agent", "collaborator");
    }

    private void insertRefs(UUID agentId, List<UUID> refereeIds, String refereeType, String refKind) {
        if (refereeIds == null) return;
        for (UUID refereeId : refereeIds) {
            AssetReferenceEntity ref = new AssetReferenceEntity();
            ref.setReferrerType(AssetType.AGENT.getValue());
            ref.setReferrerId(agentId);
            ref.setRefereeType(refereeType);
            ref.setRefereeId(refereeId);
            ref.setRefKind(refKind);
            assetReferenceMapper.insert(ref);
        }
    }

    private void importAssetReferences(UUID agentId, Map<String, Object> data,
                                       String key, String refereeType, String refKind) {
        Object raw = data.get(key);
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                UUID refId;
                try {
                    refId = UUID.fromString(item.toString());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                AssetReferenceEntity ref = new AssetReferenceEntity();
                ref.setReferrerType(AssetType.AGENT.getValue());
                ref.setReferrerId(agentId);
                ref.setRefereeType(refereeType);
                ref.setRefereeId(refId);
                ref.setRefKind(refKind);
                assetReferenceMapper.insert(ref);
            }
        }
    }

    private List<UUID> filterRefIds(List<AssetReferenceEntity> refs, String refKind) {
        return refs.stream()
                .filter(r -> refKind.equals(r.getRefKind()))
                .map(AssetReferenceEntity::getRefereeId)
                .toList();
    }

    private UUID resolveImportSourceId(String sourceType, Object sourceIdRaw, Map<?, ?> tbMap) {
        // Priority 1: exact source_id match
        if (sourceIdRaw != null) {
            try {
                return UUID.fromString(sourceIdRaw.toString());
            } catch (IllegalArgumentException ignored) {}
        }
        // Priority 2-4: source_url+tool_name / source_name+tool_name matching
        // requires ToolRegistry/McpMapper/KbMapper (not yet available in Agent module)
        // MVP: return null for non-builtin without valid source_id → marked as unresolved
        return null;
    }

    private String resolveColumn(String sortBy) {
        if ("created_at".equals(sortBy)) return "created_at";
        return "updated_at";
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
