package com.agentplatform.agent.service;

import com.agentplatform.agent.dto.CustomModelCreateRequest;
import com.agentplatform.agent.dto.CustomModelUpdateRequest;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelService {

    private static final Logger log = LoggerFactory.getLogger(ModelService.class);

    private final ModelRegistry modelRegistry;
    private final CredentialStore credentialStore;
    private final RestClient restClient;

    public ModelService(ModelRegistry modelRegistry, CredentialStore credentialStore,
                        RestClient.Builder restClientBuilder) {
        this.modelRegistry = modelRegistry;
        this.credentialStore = credentialStore;
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
                .map(this::toCustomModelVO)
                .toList();
    }

    @Transactional
    public CustomModelVO createCustomModel(CustomModelCreateRequest request, UUID userId) {
        verifyConnectivity(request.getApiUrl(), request.getApiKey());

        String apiKeyStored = credentialStore.encrypt(request.getApiKey());
        ModelInfo info = modelRegistry.createCustomModel(
                request.getName(), request.getApiUrl(),
                apiKeyStored.getBytes(), "ok", userId);
        return toCustomModelVO(info);
    }

    @Transactional
    public CustomModelVO updateCustomModel(UUID modelId, CustomModelUpdateRequest request, UUID userId) {
        String apiKeyPlain = request.getApiKey();
        byte[] apiKeyEnc = null;
        String connectionStatus = null;

        if (apiKeyPlain != null && !apiKeyPlain.isBlank()) {
            // When key changes, we must re-verify connectivity.
            // Use the request URL if provided, otherwise fall back to the existing model's URL.
            String verifyUrl = request.getApiUrl();
            if (verifyUrl == null || verifyUrl.isBlank()) {
                ModelInfo existing = modelRegistry.getById(modelId.toString())
                        .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND, "Custom model not found"));
                verifyUrl = existing.getApiUrl();
            }
            verifyConnectivity(verifyUrl, apiKeyPlain);
            apiKeyEnc = credentialStore.encrypt(apiKeyPlain).getBytes();
            connectionStatus = "ok";
        }

        ModelInfo info = modelRegistry.updateCustomModel(
                modelId, request.getName(), request.getApiUrl(),
                apiKeyEnc, connectionStatus, userId);
        return toCustomModelVO(info);
    }

    @Transactional
    public void deleteCustomModel(UUID modelId, UUID userId) {
        modelRegistry.deleteCustomModel(modelId, userId);
    }

    // ─── connectivity ───

    private void verifyConnectivity(String apiUrl, String apiKey) {
        try {
            String response = restClient.get()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);
            log.debug("Model API connectivity check passed: status from {}", apiUrl);
        } catch (Exception e) {
            log.warn("Model connectivity check failed for {}: {}", apiUrl, e.getMessage());
            throw new BizException(ErrorCode.MODEL_CONNECTION_FAILED,
                    Map.of("reason", e.getMessage()));
        }
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
