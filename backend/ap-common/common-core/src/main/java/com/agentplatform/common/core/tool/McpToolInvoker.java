package com.agentplatform.common.core.tool;

import java.util.UUID;

/**
 * Cross-module interface for executing MCP tool calls.
 * Implementation lives in ap-module-asset where McpMapper and McpClient are available.
 */
public interface McpToolInvoker {

    /**
     * Execute a tool on a remote MCP server.
     *
     * @param mcpId     the MCP source identifier
     * @param toolName  name of the tool to invoke
     * @param arguments JSON string of tool arguments
     * @return the tool execution result with status, content, and timing
     */
    ToolResult executeTool(UUID mcpId, String toolName, String arguments);
}
