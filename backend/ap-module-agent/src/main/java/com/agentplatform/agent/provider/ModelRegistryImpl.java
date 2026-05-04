package com.agentplatform.agent.provider;

import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.BuiltinModelEntity;
import com.agentplatform.agent.entity.CustomModelEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.BuiltinModelMapper;
import com.agentplatform.agent.mapper.CustomModelMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class ModelRegistryImpl implements ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistryImpl.class);

    private final BuiltinModelMapper builtinModelMapper;
    private final CustomModelMapper customModelMapper;
    private final AgentMapper agentMapper;
    private final CredentialStore credentialStore;
    private final ObjectMapper objectMapper;

    public ModelRegistryImpl(BuiltinModelMapper builtinModelMapper,
                             CustomModelMapper customModelMapper,
                             AgentMapper agentMapper,
                             CredentialStore credentialStore,
                             ObjectMapper objectMapper) {
        this.builtinModelMapper = builtinModelMapper;
        this.customModelMapper = customModelMapper;
        this.agentMapper = agentMapper;
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ModelInfo> getAllModels(UUID userId) {
        List<ModelInfo> result = new ArrayList<>();
        result.addAll(getBuiltinModels());
        result.addAll(getCustomModels(userId));
        return result;
    }

    @Override
    public Optional<ModelInfo> getById(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return Optional.empty();
        }

        // Try builtin first (builtin IDs are non-UUID strings like "gpt-4o")
        BuiltinModelEntity builtin = builtinModelMapper.selectById(modelId);
        if (builtin != null) {
            return Optional.of(toModelInfo(builtin));
        }

        // Try custom model
        try {
            UUID customId = UUID.fromString(modelId);
            CustomModelEntity custom = customModelMapper.selectById(customId);
            if (custom != null) {
                return Optional.of(toModelInfo(custom));
            }
        } catch (IllegalArgumentException ignored) {}

        return Optional.empty();
    }

    @Override
    public String getDefaultModelId() {
        List<BuiltinModelEntity> defaults = builtinModelMapper.selectList(
                new LambdaQueryWrapper<BuiltinModelEntity>()
                        .eq(BuiltinModelEntity::getIsDefault, true)
                        .eq(BuiltinModelEntity::getEnabled, true));
        if (!defaults.isEmpty()) {
            return defaults.getFirst().getId();
        }
        // Fallback to first enabled builtin
        List<BuiltinModelEntity> all = builtinModelMapper.selectList(
                new LambdaQueryWrapper<BuiltinModelEntity>()
                        .eq(BuiltinModelEntity::getEnabled, true));
        if (!all.isEmpty()) {
            return all.getFirst().getId();
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "No default model configured");
    }

    @Override
    @Transactional
    public void deleteCustomModel(UUID modelId, UUID userId) {
        CustomModelEntity model = customModelMapper.selectById(modelId);
        if (model == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "Custom model not found");
        }
        if (!model.getOwnerId().equals(userId)) {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        String defaultId = getDefaultModelId();
        String modelIdStr = modelId.toString();

        // Reset all agents using this model to the default
        agentMapper.update(null,
                new LambdaUpdateWrapper<AgentEntity>()
                        .eq(AgentEntity::getModelId, modelIdStr)
                        .set(AgentEntity::getModelId, defaultId));

        customModelMapper.deleteById(modelId);
        log.info("Deleted custom model {} and reset affected agents to default {}", modelId, defaultId);
    }

    @Override
    public List<ModelInfo> getBuiltinModels() {
        List<BuiltinModelEntity> builtins = builtinModelMapper.selectList(
                new LambdaQueryWrapper<BuiltinModelEntity>()
                        .eq(BuiltinModelEntity::getEnabled, true)
                        .orderByAsc(BuiltinModelEntity::getSortOrder));
        return builtins.stream().map(this::toModelInfo).toList();
    }

    @Override
    public List<ModelInfo> getCustomModels(UUID userId) {
        List<CustomModelEntity> customs = customModelMapper.selectList(
                new LambdaQueryWrapper<CustomModelEntity>()
                        .eq(CustomModelEntity::getOwnerId, userId)
                        .orderByDesc(CustomModelEntity::getCreatedAt));
        return customs.stream().map(this::toModelInfo).toList();
    }

    @Override
    @Transactional
    public ModelInfo createCustomModel(String name, String apiUrl, byte[] apiKeyEnc,
                                       String connectionStatus, UUID userId) {
        CustomModelEntity entity = new CustomModelEntity();
        entity.setId(UUID.randomUUID());
        entity.setOwnerId(userId);
        entity.setName(name);
        entity.setApiUrl(apiUrl);
        entity.setApiKeyEnc(apiKeyEnc);
        entity.setConnectionStatus(connectionStatus);
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        customModelMapper.insert(entity);
        log.info("Created custom model {} ({}) for user {}", entity.getId(), name, userId);
        return toModelInfo(entity);
    }

    @Override
    @Transactional
    public ModelInfo updateCustomModel(UUID modelId, String name, String apiUrl,
                                       byte[] apiKeyEnc, String connectionStatus, UUID userId) {
        CustomModelEntity entity = customModelMapper.selectById(modelId);
        if (entity == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "Custom model not found");
        }
        if (!entity.getOwnerId().equals(userId)) {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        if (name != null) entity.setName(name);
        if (apiUrl != null) entity.setApiUrl(apiUrl);
        if (apiKeyEnc != null) entity.setApiKeyEnc(apiKeyEnc);
        if (connectionStatus != null) entity.setConnectionStatus(connectionStatus);
        entity.setUpdatedAt(java.time.OffsetDateTime.now());

        customModelMapper.updateById(entity);
        log.info("Updated custom model {}", modelId);
        return toModelInfo(entity);
    }

    @Override
    public Map<String, Object> buildChatClient(ModelInfo model) {
        // MVP placeholder: returns a descriptive map instead of Spring AI ChatClient.
        // Full Spring AI integration (task 4.2) will replace this.
        return Map.of(
                "model_id", model.getId(),
                "model_name", model.getName(),
                "provider", model.getProvider() != null ? model.getProvider() : "unknown",
                "source", model.getSource().name(),
                "api_url", model.getApiUrl() != null ? model.getApiUrl() : "",
                "placeholder", true);
    }

    // ─── Conversion helpers ───

    private ModelInfo toModelInfo(BuiltinModelEntity entity) {
        ModelInfo info = new ModelInfo();
        info.setId(entity.getId());
        info.setName(entity.getName());
        info.setProvider(entity.getProvider());
        info.setSource(ModelInfo.Source.BUILTIN);
        info.setDescription(entity.getDescription());
        info.setDefault(Boolean.TRUE.equals(entity.getIsDefault()));
        info.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        info.setSortOrder(entity.getSortOrder());
        info.setConfig(parseConfig(entity.getConfig()));
        return info;
    }

    private ModelInfo toModelInfo(CustomModelEntity entity) {
        ModelInfo info = new ModelInfo();
        info.setId(entity.getId().toString());
        info.setName(entity.getName());
        info.setProvider("custom");
        info.setSource(ModelInfo.Source.CUSTOM);
        info.setApiUrl(entity.getApiUrl());
        if (entity.getApiKeyEnc() != null && entity.getApiKeyEnc().length > 0) {
            String stored = new String(entity.getApiKeyEnc());
            info.setApiKeyMasked(credentialStore.mask(stored));
        }
        info.setConnectionStatus(entity.getConnectionStatus());
        info.setLastError(entity.getLastError());
        info.setCreatedAt(entity.getCreatedAt());
        info.setUpdatedAt(entity.getUpdatedAt());
        info.setEnabled(true);
        return info;
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
