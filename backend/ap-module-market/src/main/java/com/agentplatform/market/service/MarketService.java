package com.agentplatform.market.service;

import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.entity.SkillEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.asset.mapper.SkillMapper;
import com.agentplatform.common.core.agent.SourceResolver;
import com.agentplatform.common.core.enums.AssetStatus;
import com.agentplatform.common.core.enums.AssetType;
import com.agentplatform.common.core.enums.SourceType;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.AssetVisibility;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.security.PermissionChecker;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.agentplatform.market.converter.MarketConverter;
import com.agentplatform.market.dto.*;
import com.agentplatform.market.entity.FavoriteEntity;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.entity.ReviewEntity;
import com.agentplatform.market.mapper.FavoriteMapper;
import com.agentplatform.market.mapper.MarketItemMapper;
import com.agentplatform.market.mapper.ReviewMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class MarketService {

    private static final Logger log = LoggerFactory.getLogger(MarketService.class);

    private final MarketItemMapper marketItemMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReviewMapper reviewMapper;
    private final AssetVersionMapper assetVersionMapper;
    private final AgentMapper agentMapper;
    private final AgentToolBindingMapper toolBindingMapper;
    private final AssetReferenceMapper assetReferenceMapper;
    private final SkillMapper skillMapper;
    private final McpMapper mcpMapper;
    private final MarketConverter marketConverter;
    private final SourceResolver sourceResolver;
    private final PermissionChecker permissionChecker;
    private final ObjectMapper objectMapper;

    public MarketService(MarketItemMapper marketItemMapper,
                         FavoriteMapper favoriteMapper,
                         ReviewMapper reviewMapper,
                         AssetVersionMapper assetVersionMapper,
                         AgentMapper agentMapper,
                         AgentToolBindingMapper toolBindingMapper,
                         AssetReferenceMapper assetReferenceMapper,
                         SkillMapper skillMapper,
                         McpMapper mcpMapper,
                         MarketConverter marketConverter,
                         SourceResolver sourceResolver,
                         PermissionChecker permissionChecker,
                         ObjectMapper objectMapper) {
        this.marketItemMapper = marketItemMapper;
        this.favoriteMapper = favoriteMapper;
        this.reviewMapper = reviewMapper;
        this.assetVersionMapper = assetVersionMapper;
        this.agentMapper = agentMapper;
        this.toolBindingMapper = toolBindingMapper;
        this.assetReferenceMapper = assetReferenceMapper;
        this.skillMapper = skillMapper;
        this.mcpMapper = mcpMapper;
        this.marketConverter = marketConverter;
        this.sourceResolver = sourceResolver;
        this.permissionChecker = permissionChecker;
        this.objectMapper = objectMapper;
    }

    // ───────── 11.1 Publish ─────────

    @Transactional
    public MarketItemVO publish(PublishRequest request, UUID userId) {
        AssetType assetType = resolveAssetType(request.getAssetType());

        // Build config snapshot + update asset status
        Map<String, Object> configSnapshot = buildConfigSnapshot(assetType, request.getAssetId(), userId);

        // Create asset_versions record
        AssetVersionEntity version = new AssetVersionEntity();
        version.setId(UUID.randomUUID());
        version.setAssetType(assetType.getValue());
        version.setAssetId(request.getAssetId());
        version.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "v1.0.0");
        version.setConfigSnapshot(toJson(configSnapshot));
        version.setReleaseNotes(request.getReleaseNotes());
        version.setPublishedBy(userId);
        version.setPublishedAt(OffsetDateTime.now());
        assetVersionMapper.insert(version);

        // Create or update market_item
        boolean isNewItem = false;
        MarketItemEntity item = marketItemMapper.selectOne(
                new LambdaQueryWrapper<MarketItemEntity>()
                        .eq(MarketItemEntity::getAssetType, assetType.getValue())
                        .eq(MarketItemEntity::getAssetId, request.getAssetId()));
        if (item == null) {
            isNewItem = true;
            item = new MarketItemEntity();
            item.setId(UUID.randomUUID());
            item.setAssetType(assetType.getValue());
            item.setAssetId(request.getAssetId());
            item.setAuthorId(userId);
            item.setUseCount(0L);
            item.setFavoriteCount(0L);
            item.setAvgRating(BigDecimal.ZERO);
            item.setReviewCount(0);
            item.setCreatedAt(OffsetDateTime.now());
        }
        item.setStatus("listed");
        item.setVisibility(request.getVisibility() != null ? request.getVisibility() : "public");
        item.setCurrentVersionId(version.getId());
        item.setUpdatedAt(OffsetDateTime.now());
        if (isNewItem) {
            marketItemMapper.insert(item);
        } else {
            marketItemMapper.updateById(item);
        }
        log.info("Published {} {} to market, version {}", assetType.getValue(), request.getAssetId(), version.getVersion());

        MarketItemVO vo = marketConverter.toItemVO(item);
        vo.setAuthorName(resolveAuthorName(item.getAuthorId()));
        return vo;
    }

    // ───────── 11.2 Unlist / Visibility ─────────

    @Transactional
    public MarketItemVO updateVisibility(UUID itemId, String visibility, UUID userId) {
        MarketItemEntity item = getItemOrThrow(itemId);
        if (!item.getAuthorId().equals(userId)) {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        if ("private".equalsIgnoreCase(visibility)) {
            item.setVisibility("private");
            item.setStatus("unlisted");
        } else {
            item.setVisibility(visibility);
            item.setStatus("listed");
        }
        item.setUpdatedAt(OffsetDateTime.now());
        marketItemMapper.updateById(item);

        MarketItemVO vo = marketConverter.toItemVO(item);
        vo.setAuthorName(resolveAuthorName(item.getAuthorId()));
        return vo;
    }

    // ───────── 11.3 Browse & Search ─────────

    public PageResult<MarketItemVO> listItems(String type, String category,
            String search, String tags, int page, int pageSize,
            String sortBy, String sortOrder) {
        LambdaQueryWrapper<MarketItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketItemEntity::getStatus, "listed");

        if (StringUtils.hasText(type)) {
            wrapper.eq(MarketItemEntity::getAssetType, type);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(MarketItemEntity::getCategory, category);
        }
        if (StringUtils.hasText(tags)) {
            wrapper.apply("tags @> {0}::jsonb", "\"" + tags + "\"");
        }
        if (StringUtils.hasText(search)) {
            wrapper.apply(
                "to_tsvector('simple', coalesce(category, '') || ' ' || coalesce(tags::text, ''))" +
                " @@ plainto_tsquery('simple', {0})", search);
        }

        wrapper.orderBy(true, "asc".equalsIgnoreCase(sortOrder),
                resolveSortColumn(sortBy));

        IPage<MarketItemEntity> pageResult = marketItemMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<MarketItemVO> voList = pageResult.getRecords().stream()
                .map(e -> {
                    MarketItemVO vo = marketConverter.toItemVO(e);
                    vo.setAuthorName(resolveAuthorName(e.getAuthorId()));
                    return vo;
                })
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    public MarketItemDetailVO getItemDetail(UUID itemId) {
        MarketItemEntity item = getItemOrThrow(itemId);
        MarketItemDetailVO vo = marketConverter.toItemDetailVO(item);
        vo.setAuthorName(resolveAuthorName(item.getAuthorId()));

        if (item.getCurrentVersionId() != null) {
            AssetVersionEntity version = assetVersionMapper.selectById(item.getCurrentVersionId());
            if (version != null) {
                vo.setConfigSnapshot(version.getConfigSnapshot());
            }
        }
        return vo;
    }

    public PageResult<MarketItemVO> getFeaturedItems(String type) {
        LambdaQueryWrapper<MarketItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketItemEntity::getStatus, "listed");
        if (StringUtils.hasText(type)) {
            wrapper.eq(MarketItemEntity::getAssetType, type);
        }
        wrapper.orderByDesc(MarketItemEntity::getAvgRating)
               .orderByDesc(MarketItemEntity::getFavoriteCount)
               .last("LIMIT 20");
        List<MarketItemEntity> items = marketItemMapper.selectList(wrapper);

        List<MarketItemVO> voList = items.stream()
                .map(e -> {
                    MarketItemVO vo = marketConverter.toItemVO(e);
                    vo.setAuthorName(resolveAuthorName(e.getAuthorId()));
                    return vo;
                })
                .toList();
        return new PageResult<>(voList, voList.size(), 1, 20);
    }

    // ───────── 11.4 Favorites ─────────

    @Transactional
    public void addFavorite(UUID itemId, UUID userId) {
        getItemOrThrow(itemId); // validates item exists
        boolean exists = favoriteMapper.exists(new LambdaQueryWrapper<FavoriteEntity>()
                .eq(FavoriteEntity::getUserId, userId)
                .eq(FavoriteEntity::getMarketItemId, itemId));
        if (!exists) {
            FavoriteEntity fav = new FavoriteEntity();
            fav.setUserId(userId);
            fav.setMarketItemId(itemId);
            fav.setCreatedAt(OffsetDateTime.now());
            favoriteMapper.insert(fav);
            marketItemMapper.incrementFavoriteCount(itemId);
        }
    }

    @Transactional
    public void removeFavorite(UUID itemId, UUID userId) {
        getItemOrThrow(itemId); // validates item exists
        FavoriteEntity existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<FavoriteEntity>()
                        .eq(FavoriteEntity::getUserId, userId)
                        .eq(FavoriteEntity::getMarketItemId, itemId));
        if (existing != null) {
            favoriteMapper.delete(new LambdaQueryWrapper<FavoriteEntity>()
                    .eq(FavoriteEntity::getUserId, userId)
                    .eq(FavoriteEntity::getMarketItemId, itemId));
            marketItemMapper.decrementFavoriteCount(itemId);
        }
    }

    // ───────── 11.4 Reviews ─────────

    @Transactional
    public ReviewVO createReview(UUID itemId, ReviewCreateRequest request, UUID userId) {
        getItemOrThrow(itemId); // validates item exists

        boolean exists = reviewMapper.exists(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getMarketItemId, itemId)
                .eq(ReviewEntity::getUserId, userId));
        if (exists) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "您已经评价过该商品");
        }

        ReviewEntity review = new ReviewEntity();
        review.setId(UUID.randomUUID());
        review.setMarketItemId(itemId);
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(OffsetDateTime.now());
        reviewMapper.insert(review);

        recalcRatingAndCount(itemId);

        ReviewVO vo = marketConverter.toReviewVO(review);
        vo.setUserName(resolveAuthorName(userId));
        return vo;
    }

    public List<ReviewVO> listReviews(UUID itemId, int page, int pageSize) {
        IPage<ReviewEntity> pageResult = reviewMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ReviewEntity>()
                        .eq(ReviewEntity::getMarketItemId, itemId)
                        .orderByDesc(ReviewEntity::getCreatedAt));

        return pageResult.getRecords().stream()
                .map(e -> {
                    ReviewVO vo = marketConverter.toReviewVO(e);
                    vo.setUserName(resolveAuthorName(e.getUserId()));
                    return vo;
                })
                .toList();
    }

    // ───────── 11.5 Import ─────────

    @Transactional
    public Map<String, Object> importItem(UUID itemId, UUID userId) {
        MarketItemEntity item = getItemOrThrow(itemId);
        if (item.getCurrentVersionId() == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "该商品没有可导入的版本");
        }

        AssetVersionEntity version = assetVersionMapper.selectById(item.getCurrentVersionId());
        if (version == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "版本快照不存在");
        }

        Map<String, Object> config = parseJsonToMap(version.getConfigSnapshot());
        AssetType assetType = resolveAssetType(item.getAssetType());
        UUID newAssetId = createAssetFromConfig(assetType, config, userId);

        marketItemMapper.incrementUseCount(itemId);
        log.info("Imported {} from market item {} for user {}, new asset {}", assetType.getValue(), itemId, userId, newAssetId);

        return Map.of("asset_id", newAssetId, "asset_type", assetType.getValue());
    }

    // ───────── Snapshot Builders ─────────

    private Map<String, Object> buildConfigSnapshot(AssetType assetType, UUID assetId, UUID userId) {
        return switch (assetType) {
            case AGENT -> buildAgentSnapshot(assetId, userId);
            case SKILL -> buildSkillSnapshot(assetId, userId);
            case MCP -> buildMcpSnapshot(assetId, userId);
        };
    }

    private Map<String, Object> buildAgentSnapshot(UUID agentId, UUID userId) {
        AgentEntity entity = agentMapper.selectById(agentId);
        if (entity == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        permissionChecker.checkAccess(userId, toAssetRef(entity), Permission.WRITE);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", entity.getName());
        snapshot.put("description", entity.getDescription());
        snapshot.put("avatar", entity.getAvatar());
        snapshot.put("system_prompt", entity.getSystemPrompt());
        snapshot.put("max_steps", entity.getMaxSteps());
        snapshot.put("model_id", entity.getModelId());

        List<AgentToolBindingEntity> bindings = toolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId)
                        .orderByAsc(AgentToolBindingEntity::getSortOrder));
        List<Map<String, Object>> tbList = bindings.stream().map(b -> {
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
        snapshot.put("tool_bindings", tbList);

        List<AssetReferenceEntity> refs = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getReferrerType, AssetType.AGENT.getValue())
                        .eq(AssetReferenceEntity::getReferrerId, agentId));
        snapshot.put("skill_ids", filterRefIds(refs, "skill"));
        snapshot.put("knowledge_base_ids", filterRefIds(refs, "knowledge"));
        snapshot.put("collaborator_agent_ids", filterRefIds(refs, "collaborator"));

        entity.setStatus(AssetStatus.PUBLISHED.getValue());
        entity.setHasUnpublishedChanges(false);
        agentMapper.updateById(entity);

        return snapshot;
    }

    private Map<String, Object> buildSkillSnapshot(UUID skillId, UUID userId) {
        SkillEntity entity = skillMapper.selectById(skillId);
        if (entity == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        permissionChecker.checkAccess(userId, toAssetRef(entity), Permission.WRITE);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", entity.getName());
        snapshot.put("description", entity.getDescription());
        snapshot.put("format", entity.getFormat());
        snapshot.put("content", entity.getContent());
        snapshot.put("trigger_conditions", parseJson(entity.getTriggerConditions()));

        entity.setStatus(AssetStatus.PUBLISHED.getValue());
        entity.setHasUnpublishedChanges(false);
        skillMapper.updateById(entity);

        return snapshot;
    }

    private Map<String, Object> buildMcpSnapshot(UUID mcpId, UUID userId) {
        McpEntity entity = mcpMapper.selectById(mcpId);
        if (entity == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        permissionChecker.checkAccess(userId, toAssetRef(entity), Permission.WRITE);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", entity.getName());
        snapshot.put("description", entity.getDescription());
        snapshot.put("url", entity.getUrl());
        snapshot.put("protocol", entity.getProtocol());
        snapshot.put("json_config", entity.getJsonConfig());
        snapshot.put("tools_discovered", parseJson(entity.getToolsDiscovered()));

        // MCP publishing must NOT include auth_headers_enc (R7-7)

        entity.setStatus(AssetStatus.PUBLISHED.getValue());
        entity.setHasUnpublishedChanges(false);
        mcpMapper.updateById(entity);

        return snapshot;
    }

    // ───────── Asset Import Helpers ─────────

    private UUID createAssetFromConfig(AssetType assetType, Map<String, Object> config, UUID userId) {
        return switch (assetType) {
            case AGENT -> createAgentFromConfig(config, userId);
            case SKILL -> createSkillFromConfig(config, userId);
            case MCP -> createMcpFromConfig(config, userId);
        };
    }

    private UUID createAgentFromConfig(Map<String, Object> config, UUID userId) {
        AgentEntity entity = new AgentEntity();
        entity.setOwnerId(userId);
        entity.setName((String) config.get("name"));
        entity.setDescription((String) config.get("description"));
        entity.setAvatar((String) config.get("avatar"));
        entity.setSystemPrompt((String) config.get("system_prompt"));
        entity.setMaxSteps(config.get("max_steps") != null ? ((Number) config.get("max_steps")).intValue() : 10);
        entity.setModelId((String) config.get("model_id"));
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setHasUnpublishedChanges(false);
        agentMapper.insert(entity);

        Object tbRaw = config.get("tool_bindings");
        if (tbRaw instanceof List<?> tbList) {
            int sortIdx = 0;
            for (Object item : tbList) {
                if (!(item instanceof Map<?, ?> tbMap)) continue;
                AgentToolBindingEntity binding = new AgentToolBindingEntity();
                binding.setAgentId(entity.getId());
                binding.setSourceType((String) tbMap.get("source_type"));
                binding.setSourceId(parseUuid(tbMap.get("source_id")));
                binding.setToolName((String) tbMap.get("tool_name"));
                binding.setToolSchemaSnapshot(toJson(tbMap.get("tool_schema_snapshot")));
                binding.setEnabled(tbMap.get("enabled") != null ? (Boolean) tbMap.get("enabled") : true);
                binding.setSortOrder(sortIdx++);
                binding.setCreatedAt(OffsetDateTime.now());
                binding.setUpdatedAt(OffsetDateTime.now());
                toolBindingMapper.insert(binding);
            }
        }

        importRefs(entity.getId(), config, "skill_ids", AssetType.SKILL.getValue(), "skill");
        importRefs(entity.getId(), config, "knowledge_base_ids", "knowledge_base", "knowledge");
        importRefs(entity.getId(), config, "collaborator_agent_ids", AssetType.AGENT.getValue(), "collaborator");

        return entity.getId();
    }

    private UUID createSkillFromConfig(Map<String, Object> config, UUID userId) {
        SkillEntity entity = new SkillEntity();
        entity.setOwnerId(userId);
        entity.setName((String) config.get("name"));
        entity.setDescription((String) config.get("description"));
        entity.setFormat((String) config.get("format"));
        entity.setContent((String) config.get("content"));
        entity.setTriggerConditions(toJson(config.get("trigger_conditions")));
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setHasUnpublishedChanges(false);
        skillMapper.insert(entity);
        return entity.getId();
    }

    private UUID createMcpFromConfig(Map<String, Object> config, UUID userId) {
        McpEntity entity = new McpEntity();
        entity.setOwnerId(userId);
        entity.setName((String) config.get("name"));
        entity.setDescription((String) config.get("description"));
        entity.setUrl((String) config.get("url"));
        entity.setProtocol((String) config.get("protocol"));
        entity.setJsonConfig((String) config.get("json_config"));
        entity.setEnabled(true);
        entity.setConnectionStatus("offline");
        entity.setStatus(AssetStatus.DRAFT.getValue());
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setHasUnpublishedChanges(false);
        // MCP import explicitly does NOT copy auth headers (R7-4)
        mcpMapper.insert(entity);
        return entity.getId();
    }

    // ───────── Helpers ─────────

    private MarketItemEntity getItemOrThrow(UUID itemId) {
        MarketItemEntity item = marketItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        return item;
    }

    private AssetType resolveAssetType(String type) {
        try {
            return AssetType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "不支持的资产类型: " + type);
        }
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

    private AssetRef toAssetRef(SkillEntity entity) {
        AssetVisibility visibility;
        try {
            visibility = AssetVisibility.valueOf(entity.getVisibility().toUpperCase());
        } catch (IllegalArgumentException e) {
            visibility = AssetVisibility.PRIVATE;
        }
        return new AssetRef(entity.getId(), entity.getOwnerId(), visibility, entity.getDeletedAt());
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

    private List<String> filterRefIds(List<AssetReferenceEntity> refs, String refKind) {
        return refs.stream()
                .filter(r -> refKind.equals(r.getRefKind()))
                .map(r -> r.getRefereeId().toString())
                .toList();
    }

    private void importRefs(UUID agentId, Map<String, Object> config, String key,
                             String refereeType, String refKind) {
        Object raw = config.get(key);
        if (raw instanceof List<?> ids) {
            for (Object idObj : ids) {
                UUID refId = parseUuid(idObj);
                if (refId == null) continue;
                AssetReferenceEntity ref = new AssetReferenceEntity();
                ref.setId(UUID.randomUUID());
                ref.setReferrerType(AssetType.AGENT.getValue());
                ref.setReferrerId(agentId);
                ref.setRefereeType(refereeType);
                ref.setRefereeId(refId);
                ref.setRefKind(refKind);
                assetReferenceMapper.insert(ref);
            }
        }
    }

    private void recalcRatingAndCount(UUID itemId) {
        marketItemMapper.recalcRatingAndCount(itemId);
    }

    private String resolveAuthorName(UUID userId) {
        if (userId == null) return null;
        // MVP: Use the owner name from the first available source.
        // In a full implementation, this would use a UserResolver service.
        return "User-" + userId.toString().substring(0, 8);
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<MarketItemEntity, ?> resolveSortColumn(String sortBy) {
        return switch (sortBy) {
            case "use_count" -> MarketItemEntity::getUseCount;
            case "favorite_count" -> MarketItemEntity::getFavoriteCount;
            case "avg_rating" -> MarketItemEntity::getAvgRating;
            case "created_at" -> MarketItemEntity::getCreatedAt;
            default -> MarketItemEntity::getUpdatedAt;
        };
    }

    // ─── JSON helpers ───

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private UUID parseUuid(Object obj) {
        if (obj == null) return null;
        try {
            return UUID.fromString(obj.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
