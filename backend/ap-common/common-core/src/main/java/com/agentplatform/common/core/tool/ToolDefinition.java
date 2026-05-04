package com.agentplatform.common.core.tool;

import com.agentplatform.common.core.enums.SourceType;

import java.util.Objects;
import java.util.UUID;

/**
 * Tool definition as stored in the tool registry (§3.9.1).
 */
public class ToolDefinition {

    private String toolId;
    private String toolName;
    private SourceType sourceType;
    private UUID sourceId;
    private String description;
    private String parameters;
    private boolean enabled = true;

    public ToolDefinition() {}

    public ToolDefinition(String toolId, String toolName, SourceType sourceType, UUID sourceId,
                          String description, String parameters) {
        this.toolId = toolId;
        this.toolName = toolName;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = description;
        this.parameters = parameters;
    }

    public String getToolId() { return toolId; }
    public void setToolId(String toolId) { this.toolId = toolId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // ─── Helpers for tool_id generation ───

    public static String builtinId(String toolName) {
        return "builtin_" + toolName;
    }

    public static String mcpId(UUID mcpId, String toolName) {
        return "mcp_" + mcpId + "_" + toolName;
    }

    public static String knowledgeId(UUID kbId) {
        return "knowledge_" + kbId;
    }

    // ─── equals / hashCode / toString ───

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolDefinition that)) return false;
        return Objects.equals(toolId, that.toolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolId);
    }

    @Override
    public String toString() {
        return "ToolDefinition{toolId='" + toolId + "', toolName='" + toolName + "', sourceType=" + sourceType + '}';
    }
}
