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
     * MVP placeholder: returns a ChatClient-like wrapper map.
     * Full Spring AI ChatClient integration deferred to post-MVP.
     */
    Map<String, Object> buildChatClient(ModelInfo model);
}
