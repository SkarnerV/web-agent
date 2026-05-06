package com.agentplatform.agent.service;

import com.agentplatform.agent.dto.CustomModelCreateRequest;
import com.agentplatform.agent.dto.CustomModelUpdateRequest;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ModelService {

    private static final Logger log = LoggerFactory.getLogger(ModelService.class);
    private static final Pattern API_KEY_PATTERN = Pattern.compile("\\bsk-[A-Za-z0-9_-]+\\b");

    private final ModelRegistry modelRegistry;
    private final CredentialStore credentialStore;
    private final AgentMapper agentMapper;
    private final RestClient restClient;

    public ModelService(ModelRegistry modelRegistry, CredentialStore credentialStore,
                        AgentMapper agentMapper, RestClient.Builder restClientBuilder) {
        this.modelRegistry = modelRegistry;
        this.credentialStore = credentialStore;
        this.agentMapper = agentMapper;
        this.restClient = restClientBuilder.build();
    }

    public List<BuiltinModelVO> listBuiltinModels() {
        return modelRegistry.getBuiltinModels().stream()
                .map(this::toBuiltinModelVO)
                .toList();
    }

    public List<ModelInfo> listAllModels(UUID userId) {
        return modelRegistry.getAllModels(userId);
    }

    public List<CustomModelVO> listCustomModels(UUID userId) {
        return modelRegistry.getCustomModels(userId).stream()
                .map(info -> {
                    CustomModelVO vo = toCustomModelVO(info);
                    vo.setAgentCount(countAgentsByModelId(info.getId()));
                    return vo;
                })
                .toList();
    }

    public int countAgentsByModelId(String modelId) {
        return Math.toIntExact(agentMapper.selectCount(
                new LambdaQueryWrapper<AgentEntity>()
                        .eq(AgentEntity::getModelId, modelId)));
    }

    public List<String> listAgentNamesByModelId(String modelId) {
        return agentMapper.selectList(
                new LambdaQueryWrapper<AgentEntity>()
                        .eq(AgentEntity::getModelId, modelId)
                        .select(AgentEntity::getName))
                .stream()
                .map(AgentEntity::getName)
                .toList();
    }

    @Transactional
    public CustomModelVO createCustomModel(CustomModelCreateRequest request, UUID userId) {
        String normalizedApiUrl = normalizeApiUrl(request.getApiUrl());
        verifyConnectivity(normalizedApiUrl, request.getApiKey());

        String apiKeyStored = credentialStore.encrypt(request.getApiKey());
        ModelInfo info = modelRegistry.createCustomModel(
                request.getName(), normalizedApiUrl,
                apiKeyStored.getBytes(), "connected", userId);
        return toCustomModelVO(info);
    }

    @Transactional
    public CustomModelVO updateCustomModel(UUID modelId, CustomModelUpdateRequest request, UUID userId) {
        String apiKeyPlain = request.getApiKey();
        byte[] apiKeyEnc = null;
        String connectionStatus = null;

        ModelInfo existing = modelRegistry.getById(modelId.toString())
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND, "Custom model not found"));

        String normalizedRequestUrl = request.getApiUrl() == null || request.getApiUrl().isBlank()
                ? null
                : normalizeApiUrl(request.getApiUrl());
        String existingApiUrl = normalizeApiUrl(existing.getApiUrl());
        boolean hasNewKey = apiKeyPlain != null && !apiKeyPlain.isBlank();
        boolean hasNewUrl = normalizedRequestUrl != null && !normalizedRequestUrl.equals(existingApiUrl);

        if (hasNewKey || hasNewUrl) {
            String verifyUrl = hasNewUrl ? normalizedRequestUrl : existingApiUrl;
            String verifyKey;
            if (hasNewKey) {
                verifyKey = apiKeyPlain;
            } else {
                verifyKey = credentialStore.decrypt(new String(
                        modelRegistry.getRawApiKeyEnc(modelId)));
            }
            verifyConnectivity(verifyUrl, verifyKey);
            connectionStatus = "connected";
            if (hasNewKey) {
                apiKeyEnc = credentialStore.encrypt(apiKeyPlain).getBytes();
            }
        }

        ModelInfo info = modelRegistry.updateCustomModel(
                modelId, request.getName(), normalizedRequestUrl,
                apiKeyEnc, connectionStatus, userId);
        return toCustomModelVO(info);
    }

    @Transactional
    public void deleteCustomModel(UUID modelId, UUID userId) {
        modelRegistry.deleteCustomModel(modelId, userId);
    }

    // ─── connectivity ───

    private void verifyConnectivity(String apiUrl, String apiKey) {
        String checkUrl = apiUrl + "/models";
        try {
            restClient.get()
                    .uri(checkUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);
            log.debug("Model API connectivity check passed: {}", checkUrl);
        } catch (RestClientResponseException e) {
            String reason = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
            String responseBody = sanitizeReason(e.getResponseBodyAsString());
            if (!responseBody.isBlank()) {
                reason = reason + ": " + responseBody;
            }
            log.warn("Model connectivity check failed for {}: {}", checkUrl, reason);
            throw new BizException(ErrorCode.MODEL_CONNECTION_FAILED,
                    Map.of("reason", reason, "checkUrl", checkUrl));
        } catch (Exception e) {
            String reason = sanitizeReason(e.getMessage());
            log.warn("Model connectivity check failed for {}: {}", checkUrl, reason);
            throw new BizException(ErrorCode.MODEL_CONNECTION_FAILED,
                    Map.of("reason", reason, "checkUrl", checkUrl));
        }
    }

    private String normalizeApiUrl(String apiUrl) {
        if (apiUrl == null) {
            return "";
        }
        String normalized = apiUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.toLowerCase().endsWith("/models")) {
            normalized = normalized.substring(0, normalized.length() - "/models".length());
        }
        return normalized;
    }

    private String sanitizeReason(String reason) {
        if (reason == null) {
            return "";
        }
        return API_KEY_PATTERN.matcher(reason.replace("<EOL>", " ").replaceAll("\\s+", " ").trim())
                .replaceAll("sk-***");
    }

    // ─── converters ───

    private BuiltinModelVO toBuiltinModelVO(ModelInfo info) {
        BuiltinModelVO vo = new BuiltinModelVO();
        vo.setId(info.getId());
        vo.setName(info.getName());
        vo.setProvider(info.getProvider());
        vo.setDescription(info.getDescription());
        vo.setIsDefault(info.isDefault());
        vo.setEnabled(info.isEnabled());
        vo.setSortOrder(info.getSortOrder());
        return vo;
    }

    private CustomModelVO toCustomModelVO(ModelInfo info) {
        CustomModelVO vo = new CustomModelVO();
        vo.setId(UUID.fromString(info.getId()));
        vo.setName(info.getName());
        vo.setApiUrl(info.getApiUrl());
        vo.setApiKeyMasked(info.getApiKeyMasked());
        vo.setConnectionStatus(info.getConnectionStatus());
        vo.setLastError(info.getLastError());
        vo.setCreatedAt(info.getCreatedAt());
        vo.setUpdatedAt(info.getUpdatedAt());
        return vo;
    }
}
