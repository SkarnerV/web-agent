package com.agentplatform.chat.tool;

/**
 * Result of a tool execution.
 */
public record ToolResult(
        String toolCallId,
        String status,
        String content,
        long durationMs
) {
    public static ToolResult success(String toolCallId, String content, long durationMs) {
        return new ToolResult(toolCallId, "success", content, durationMs);
    }

    public static ToolResult error(String toolCallId, String message, long durationMs) {
        return new ToolResult(toolCallId, "error", message, durationMs);
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
