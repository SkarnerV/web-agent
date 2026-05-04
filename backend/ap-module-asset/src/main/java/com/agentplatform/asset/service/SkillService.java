package com.agentplatform.asset.service;

import com.agentplatform.asset.converter.SkillConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.SkillEntity;
import com.agentplatform.asset.mapper.SkillMapper;
import com.agentplatform.common.core.enums.AssetStatus;
import com.agentplatform.common.core.enums.AssetType;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.*;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.util.*;

@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final SkillMapper skillMapper;
    private final SkillConverter skillConverter;
    private final AssetReferenceMapper assetReferenceMapper;
    private final PermissionChecker permissionChecker;

    public SkillService(SkillMapper skillMapper,
                        SkillConverter skillConverter,
                        AssetReferenceMapper assetReferenceMapper,
                        PermissionChecker permissionChecker) {
        this.skillMapper = skillMapper;
        this.skillConverter = skillConverter;
        this.assetReferenceMapper = assetReferenceMapper;
        this.permissionChecker = permissionChecker;
    }

    @Transactional
    public SkillDetailVO create(SkillCreateRequest request, UUID currentUserId) {
        if ("yaml".equals(request.getFormat())) {
            validateYaml(request.getContent());
        }

        SkillEntity entity = skillConverter.toEntity(request);
        entity.setOwnerId(currentUserId);
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setCurrentVersion("v1.0.0");
        entity.setHasUnpublishedChanges(false);
        skillMapper.insert(entity);

        return getDetail(entity.getId(), currentUserId);
    }

    public PageResult<SkillSummaryVO> list(UUID currentUserId, String search,
                                            int page, int pageSize, String sortBy, String sortOrder) {
        LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillEntity::getOwnerId, currentUserId);
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(SkillEntity::getName, search)
                    .or().like(SkillEntity::getDescription, search));
        }
        String column = "created_at".equals(sortBy) ? "created_at" : "updated_at";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column.equals("created_at") ? SkillEntity::getCreatedAt : SkillEntity::getUpdatedAt);
        } else {
            wrapper.orderByDesc(column.equals("created_at") ? SkillEntity::getCreatedAt : SkillEntity::getUpdatedAt);
        }

        IPage<SkillEntity> pageResult = skillMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<SkillSummaryVO> voList = pageResult.getRecords().stream()
                .map(skillConverter::toSummaryVO)
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    public SkillDetailVO getDetail(UUID skillId, UUID currentUserId) {
        SkillEntity entity = getEntityOrThrow(skillId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);
        return skillConverter.toDetailVO(entity);
    }

    @Transactional
    public SkillDetailVO update(UUID skillId, SkillUpdateRequest request, UUID currentUserId) {
        SkillEntity entity = getEntityOrThrow(skillId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.WRITE);

        if (request.getVersion() != null && !request.getVersion().equals(entity.getVersion())) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getTriggerConditions() != null) entity.setTriggerConditions(request.getTriggerConditions());
        if (request.getFormat() != null) entity.setFormat(request.getFormat());
        if (request.getContent() != null) entity.setContent(request.getContent());
        if ("yaml".equals(entity.getFormat()) && request.getContent() != null) {
            validateYaml(entity.getContent());
        }

        if (AssetStatus.PUBLISHED.getValue().equals(entity.getStatus())) {
            entity.setHasUnpublishedChanges(true);
        }

        int rows = skillMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        return getDetail(skillId, currentUserId);
    }

    @Transactional
    public void delete(UUID skillId, UUID currentUserId, boolean force) {
        SkillEntity entity = getEntityOrThrow(skillId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.DELETE);

        List<AssetReferenceEntity> dependents = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getRefereeType, AssetType.SKILL.getValue())
                        .eq(AssetReferenceEntity::getRefereeId, skillId));

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
                    .eq(AssetReferenceEntity::getRefereeType, AssetType.SKILL.getValue())
                    .eq(AssetReferenceEntity::getRefereeId, skillId));
        }

        skillMapper.deleteById(skillId);
        log.info("Deleted skill {} (force={})", skillId, force);
    }

    public Map<String, Object> export(UUID skillId, UUID currentUserId) {
        SkillEntity entity = getEntityOrThrow(skillId);
        AssetRef ref = toAssetRef(entity);
        permissionChecker.checkAccess(currentUserId, ref, Permission.READ);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", entity.getName());
        data.put("description", entity.getDescription());
        data.put("format", entity.getFormat());
        data.put("content", entity.getContent());
        data.put("trigger_conditions", entity.getTriggerConditions());
        data.put("current_version", entity.getCurrentVersion());
        return data;
    }

    // ─── helpers ───

    private SkillEntity getEntityOrThrow(UUID skillId) {
        SkillEntity entity = skillMapper.selectById(skillId);
        if (entity == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        return entity;
    }

    private AssetRef toAssetRef(SkillEntity entity) {
        AssetVisibility visibility;
        try {
            visibility = AssetVisibility.valueOf(entity.getVisibility().toUpperCase());
        } catch (IllegalArgumentException e) {
            visibility = AssetVisibility.PRIVATE;
        }
        return new AssetRef(entity.getId(), entity.getOwnerId(), visibility, entity.getDeletedAt());
    }

    private void validateYaml(String content) {
        try {
            new Yaml(new SafeConstructor(new LoaderOptions())).load(content);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Invalid YAML content: " + e.getMessage()));
        }
    }
}
