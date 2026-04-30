package com.agentplatform.chat.llm;

/**
 * Represents a single chunk from the LLM stream.
 */
public sealed interface LlmChunk {

    record TokenChunk(String delta) implements LlmChunk {}

    record ToolCallChunk(String toolCallId, String toolName, String arguments) implements LlmChunk {}

    record FinishChunk(String finishReason, int promptTokens, int completionTokens) implements LlmChunk {}

    record ErrorChunk(String code, String message) implements LlmChunk {}
}
