package com.agentplatform.chat.llm;

import java.util.Map;

/**
 * Represents a message in the LLM conversation context.
 */
public record LlmMessage(
        String role,
        String content,
        Map<String, Object> toolResult
) {
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null);
    }

    public static LlmMessage toolResult(String toolCallId, String result) {
        return new LlmMessage("tool", result, Map.of("tool_call_id", toolCallId));
    }
}
