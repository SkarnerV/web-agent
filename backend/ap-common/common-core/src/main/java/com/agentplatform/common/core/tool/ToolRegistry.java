package com.agentplatform.common.core.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module registry for all available tools: builtin, MCP, and knowledge.
 * Implementations live in ap-module-asset where MCP/Skill/Knowledge mappers are available.
 */
public interface ToolRegistry {

    /**
     * Returns all tools available to the given user (builtin + MCP + knowledge).
     */
    List<ToolDefinition> getAvailableTools(UUID userId);

    /**
     * Looks up a single tool by its composite id.
     */
    Optional<ToolDefinition> getToolById(String toolId);

    /**
     * Validates each binding exists and enriches it with a tool_schema_snapshot.
     * Throws BizException if a tool is not found or belongs to a disabled MCP.
     *
     * @param bindings raw tool binding requests from the agent config
     * @param userId   current user for permission-aware resolution
     * @return list of snapshot maps with keys: source_type, source_id, tool_name, tool_schema_snapshot
     */
    List<Map<String, Object>> resolveAgentTools(List<Map<String, Object>> bindings, UUID userId);

    /**
     * Re-reads tools_discovered from the MCP entity and re-registers them.
     */
    void refreshMcpTools(UUID mcpId);

    /**
     * Programmatically registers a builtin tool (called at startup).
     */
    void registerBuiltinTool(ToolDefinition def);
}
