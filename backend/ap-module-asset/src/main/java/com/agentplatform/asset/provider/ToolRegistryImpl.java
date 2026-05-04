package com.agentplatform.asset.provider;

import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.enums.SourceType;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.tool.ToolDefinition;
import com.agentplatform.common.core.tool.ToolRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistryImpl implements ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryImpl.class);

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final McpMapper mcpMapper;
    private final ObjectMapper objectMapper;

    public ToolRegistryImpl(McpMapper mcpMapper, ObjectMapper objectMapper) {
        this.mcpMapper = mcpMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void registerBuiltinTools() {
        registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("web_search"),
                "web_search",
                SourceType.BUILTIN,
                null,
                "Search the web for current information",
                jsonSchema(webSearchSchema())));
        registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("calculator"),
                "calculator",
                SourceType.BUILTIN,
                null,
                "Evaluate mathematical expressions",
                jsonSchema(calculatorSchema())));
        registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("file_read"),
                "file_read",
                SourceType.BUILTIN,
                null,
                "Read content from a file",
                jsonSchema(fileReadSchema())));
        log.info("Registered {} builtin tools", tools.size());
    }

    @Override
    public List<ToolDefinition> getAvailableTools(UUID userId) {
        List<ToolDefinition> result = new ArrayList<>();
        for (ToolDefinition def : tools.values()) {
            if (def.isEnabled()) {
                result.add(def);
            }
        }
        return result;
    }

    @Override
    public Optional<ToolDefinition> getToolById(String toolId) {
        return Optional.ofNullable(tools.get(toolId));
    }

    @Override
    public List<Map<String, Object>> resolveAgentTools(List<Map<String, Object>> bindings, UUID userId) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> resolved = new ArrayList<>();
        for (Map<String, Object> binding : bindings) {
            String toolName = (String) binding.get("tool_name");
            String sourceTypeRaw = (String) binding.get("source_type");

            SourceType sourceType;
            try {
                sourceType = SourceType.valueOf(sourceTypeRaw.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "Unknown source_type: " + sourceTypeRaw));
            }

            Object sourceIdRaw = binding.get("source_id");
            UUID sourceId = null;
            if (sourceIdRaw instanceof UUID uuid) {
                sourceId = uuid;
            } else if (sourceIdRaw != null) {
                try {
                    sourceId = UUID.fromString(sourceIdRaw.toString());
                } catch (IllegalArgumentException ignored) {}
            }

            if (sourceType != SourceType.BUILTIN && sourceId == null) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "source_id is required for source_type: " + sourceTypeRaw));
            }

            ToolDefinition def = switch (sourceType) {
                case BUILTIN -> tools.get(ToolDefinition.builtinId(toolName));
                case MCP -> {
                    var d = tools.get(ToolDefinition.mcpId(sourceId, toolName));
                    if (d != null) {
                        McpEntity mcp = mcpMapper.selectById(sourceId);
                        if (mcp == null || !Boolean.TRUE.equals(mcp.getEnabled())) {
                            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                                    Map.of("reason", "MCP is disabled or deleted", "mcp_id", sourceId));
                        }
                    }
                    yield d;
                }
                case KNOWLEDGE -> tools.get(ToolDefinition.knowledgeId(sourceId));
            };

            if (def == null || !def.isEnabled()) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "Tool not found or disabled: " + toolName));
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("source_type", sourceType.getValue());
            snapshot.put("source_id", sourceId != null ? sourceId.toString() : null);
            snapshot.put("tool_name", toolName);
            snapshot.put("tool_schema_snapshot", def.getParameters());
            resolved.add(snapshot);
        }
        return resolved;
    }

    @Override
    public void refreshMcpTools(UUID mcpId) {
        // Remove existing MCP tools for this source
        tools.entrySet().removeIf(e ->
                e.getValue().getSourceType() == SourceType.MCP && mcpId.equals(e.getValue().getSourceId()));

        McpEntity mcp = mcpMapper.selectById(mcpId);
        if (mcp == null || !Boolean.TRUE.equals(mcp.getEnabled())) {
            log.info("MCP {} not found or disabled, tools removed from registry", mcpId);
            return;
        }

        String toolsDiscovered = mcp.getToolsDiscovered();
        if (toolsDiscovered == null || toolsDiscovered.isBlank()) {
            return;
        }

        try {
            List<Map<String, Object>> toolList = objectMapper.readValue(toolsDiscovered, new TypeReference<>() {});
            for (Map<String, Object> toolData : toolList) {
                String toolName = (String) toolData.get("name");
                String description = (String) toolData.getOrDefault("description", "");
                Object params = toolData.getOrDefault("parameters", Map.of());

                ToolDefinition def = new ToolDefinition(
                        ToolDefinition.mcpId(mcpId, toolName),
                        toolName,
                        SourceType.MCP,
                        mcpId,
                        description,
                        params instanceof String s ? s : objectMapper.writeValueAsString(params));
                def.setEnabled(true);
                tools.put(def.getToolId(), def);
            }
            log.info("Refreshed {} MCP tools from {}", toolList.size(), mcpId);
        } catch (Exception e) {
            log.error("Failed to parse tools_discovered for MCP {}", mcpId, e);
        }
    }

    @Override
    public void registerBuiltinTool(ToolDefinition def) {
        def.setEnabled(true);
        tools.put(def.getToolId(), def);
    }

    // ─── JSON helpers ───

    private String jsonSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> webSearchSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "The search query"),
                        "num_results", Map.of("type", "integer", "description", "Number of results to return", "default", 5)),
                "required", List.of("query"));
    }

    private Map<String, Object> calculatorSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of("type", "string", "description", "Mathematical expression to evaluate")),
                "required", List.of("expression"));
    }

    private Map<String, Object> fileReadSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "file_path", Map.of("type", "string", "description", "Path to the file to read"),
                        "encoding", Map.of("type", "string", "description", "File encoding", "default", "utf-8")),
                "required", List.of("file_path"));
    }
}
