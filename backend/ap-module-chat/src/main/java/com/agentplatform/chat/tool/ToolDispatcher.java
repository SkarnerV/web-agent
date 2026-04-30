package com.agentplatform.chat.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Routes tool calls to the appropriate handler based on source type.
 * Supports: builtin tools, MCP remote tools, knowledge base retrieval.
 */
@Component
public class ToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ToolDispatcher.class);
    private static final long TOOL_TIMEOUT_MS = 30_000;

    /**
     * Dispatches a tool call to the appropriate handler.
     *
     * @param toolCallId  unique id for this tool call
     * @param toolName    name of the tool to invoke
     * @param arguments   JSON string of tool arguments
     * @param agentToolBindings  agent's tool binding configuration (source_type, source_id, etc.)
     * @return the tool execution result
     */
    public ToolResult dispatch(String toolCallId, String toolName, String arguments,
                               Map<String, Object> agentToolBindings) {
        long startTime = System.currentTimeMillis();
        try {
            String sourceType = resolveSourceType(toolName, agentToolBindings);

            return switch (sourceType) {
                case "builtin" -> executeBuiltinTool(toolCallId, toolName, arguments, startTime);
                case "mcp" -> executeMcpTool(toolCallId, toolName, arguments, agentToolBindings, startTime);
                case "knowledge" -> executeKnowledgeTool(toolCallId, toolName, arguments, agentToolBindings, startTime);
                default -> ToolResult.error(toolCallId,
                        "Unknown tool source type: " + sourceType,
                        System.currentTimeMillis() - startTime);
            };
        } catch (Exception e) {
            log.error("Tool dispatch failed: tool={}, error={}", toolName, e.getMessage(), e);
            return ToolResult.error(toolCallId, e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private String resolveSourceType(String toolName, Map<String, Object> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return "builtin";
        }
        Object sourceType = bindings.get("source_type");
        return sourceType != null ? sourceType.toString() : "builtin";
    }

    private ToolResult executeBuiltinTool(String toolCallId, String toolName,
                                          String arguments, long startTime) {
        // TODO: Route to registered builtin tool implementations
        log.info("Executing builtin tool: {}", toolName);
        long duration = System.currentTimeMillis() - startTime;
        return ToolResult.success(toolCallId, "Builtin tool '" + toolName + "' executed successfully.", duration);
    }

    private ToolResult executeMcpTool(String toolCallId, String toolName,
                                      String arguments, Map<String, Object> bindings, long startTime) {
        // TODO: Forward to remote MCP server via SSE/Streamable HTTP (depends on task 7.3)
        // Resilience4j retry: 2s interval, max 2 retries, 30s timeout
        Object sourceId = bindings.get("source_id");
        log.info("Executing MCP tool: {} (source_id={})", toolName, sourceId);
        long duration = System.currentTimeMillis() - startTime;
        return ToolResult.success(toolCallId, "MCP tool '" + toolName + "' executed.", duration);
    }

    private ToolResult executeKnowledgeTool(String toolCallId, String toolName,
                                            String arguments, Map<String, Object> bindings, long startTime) {
        // TODO: Call KnowledgeBaseService.search() (depends on task 8.3)
        Object sourceId = bindings.get("source_id");
        log.info("Executing knowledge tool: {} (kb_id={})", toolName, sourceId);
        long duration = System.currentTimeMillis() - startTime;
        return ToolResult.success(toolCallId, "Knowledge retrieval completed.", duration);
    }
}
