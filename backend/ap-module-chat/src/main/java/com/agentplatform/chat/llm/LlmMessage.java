package com.agentplatform.chat.llm;

import java.util.List;
import java.util.Map;

/**
 * Represents a message in the LLM conversation context.
 */
public record LlmMessage(
        String role,
        String content,
        String reasoningContent,
        List<LlmToolCall> toolCalls,
        Map<String, Object> toolResult
) {
    public record LlmToolCall(String id, String name, String arguments) {}

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null, null);
    }

    public static LlmMessage assistantToolCalls(String content, List<LlmToolCall> toolCalls) {
        return assistantToolCalls(content, toolCalls, null);
    }

    public static LlmMessage assistantToolCalls(String content, List<LlmToolCall> toolCalls,
                                                String reasoningContent) {
        return new LlmMessage("assistant", content, reasoningContent, toolCalls, null);
    }

    public static LlmMessage toolResult(String toolCallId, String result) {
        return new LlmMessage("tool", result, null, null, Map.of("tool_call_id", toolCallId));
    }
}
