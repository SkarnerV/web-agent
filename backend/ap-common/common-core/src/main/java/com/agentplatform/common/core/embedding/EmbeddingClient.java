package com.agentplatform.common.core.embedding;

import java.util.List;

/**
 * Cross-module interface for text embedding (vectorization).
 * MVP stub returns random 1536-dim vectors; production calls OpenAI / compatible API.
 */
public interface EmbeddingClient {

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text the input text
     * @return embedding vector (1536 dimensions for OpenAI-compatible models)
     */
    List<Float> embed(String text);
}
