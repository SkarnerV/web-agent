package com.agentplatform.chat.tool;

import com.agentplatform.common.core.tool.McpToolInvoker;
import com.agentplatform.common.core.tool.ToolResult;
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

    private final McpToolInvoker mcpToolInvoker;

    public ToolDispatcher(McpToolInvoker mcpToolInvoker) {
        this.mcpToolInvoker = mcpToolInvoker;
    }

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
        Object sourceIdRaw = bindings.get("source_id");
        if (sourceIdRaw == null) {
            return ToolResult.error(toolCallId, "MCP tool binding missing source_id",
                    System.currentTimeMillis() - startTime);
        }

        UUID mcpId;
        try {
            mcpId = sourceIdRaw instanceof UUID uuid ? uuid : UUID.fromString(sourceIdRaw.toString());
        } catch (IllegalArgumentException e) {
            return ToolResult.error(toolCallId, "Invalid MCP source_id: " + sourceIdRaw,
                    System.currentTimeMillis() - startTime);
        }

        log.info("Dispatching MCP tool: {} (mcp_id={})", toolName, mcpId);
        ToolResult result = mcpToolInvoker.executeTool(mcpId, toolName, arguments);

        if (!result.isSuccess()) {
            log.warn("MCP tool call failed: tool={}, mcp={}, error={}",
                    toolName, mcpId, result.content());
        }

        return result;
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
