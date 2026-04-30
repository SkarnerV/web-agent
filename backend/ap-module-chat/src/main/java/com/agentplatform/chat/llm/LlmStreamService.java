package com.agentplatform.chat.llm;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstracts LLM streaming interaction.
 * Backed by Spring AI ChatClient in the real implementation.
 */
public interface LlmStreamService {

    /**
     * Initiates a streaming chat request to the LLM.
     *
     * @param modelId   the model identifier
     * @param messages  conversation messages (role + content)
     * @param tools     tool definitions to pass to the LLM
     * @return an iterator that yields LlmChunk items as they arrive
     */
    Iterator<LlmChunk> stream(String modelId, List<LlmMessage> messages, List<Map<String, Object>> tools);
}
