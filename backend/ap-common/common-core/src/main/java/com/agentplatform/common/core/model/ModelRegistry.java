package com.agentplatform.common.core.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module registry for LLM models (§3.9.2).
 * Merges builtin models (from seed data) with per-user custom models.
 * Implementation lives in ap-module-agent where model mappers are available.
 */
public interface ModelRegistry {

    /**
     * Returns all available models for the given user (builtin + custom merged).
     */
    List<ModelInfo> getAllModels(UUID userId);

    /**
     * Looks up a single model by its id (builtin prefix or custom UUID).
     */
    Optional<ModelInfo> getById(String modelId);

    /**
     * Deletes a custom model and resets all agents that were using it back to the default model.
     *
     * @param modelId the custom model UUID
     * @param userId  owner of the custom model
     */
    void deleteCustomModel(UUID modelId, UUID userId);

    /**
     * Returns the id of the current default model.
     */
    String getDefaultModelId();

    /**
     * Returns enabled builtin models only, ordered by sort_order.
     */
    List<ModelInfo> getBuiltinModels();

    /**
     * Returns custom models belonging to the given user.
     */
    List<ModelInfo> getCustomModels(UUID userId);

    /**
     * Creates a custom model with encrypted API key and connection status.
     *
     * @param apiKeyEnc encrypted API key bytes (already encrypted by CredentialStore)
     * @param connectionStatus result of connectivity verification (e.g. "ok" or "failed")
     */
    ModelInfo createCustomModel(String name, String apiUrl, byte[] apiKeyEnc,
                                String connectionStatus, UUID userId);

    /**
     * Updates a custom model. Only non-null fields are applied.
     *
     * @param apiKeyEnc encrypted API key bytes (already encrypted by CredentialStore), null to keep existing
     * @param connectionStatus result of re-verification, null to keep existing
     */
    ModelInfo updateCustomModel(UUID modelId, String name, String apiUrl,
                                byte[] apiKeyEnc, String connectionStatus, UUID userId);

    /**
     * MVP placeholder: returns a ChatClient-like wrapper map.
     * Full Spring AI ChatClient integration deferred to post-MVP.
     */
    Map<String, Object> buildChatClient(ModelInfo model);
}
