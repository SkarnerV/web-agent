package com.agentplatform.asset.provider;

import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.tool.ToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolRegistryImplTest {

    @Mock private McpMapper mcpMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ToolRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistryImpl(mcpMapper, objectMapper);
        // Manually register builtins since @PostConstruct doesn't fire in pure Mockito tests
        registry.registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("web_search"), "web_search",
                com.agentplatform.common.core.enums.SourceType.BUILTIN, null,
                "Search the web", "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}"));
        registry.registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("calculator"), "calculator",
                com.agentplatform.common.core.enums.SourceType.BUILTIN, null,
                "Evaluate math", "{\"type\":\"object\",\"properties\":{\"expression\":{\"type\":\"string\"}}}"));
        registry.registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("file_read"), "file_read",
                com.agentplatform.common.core.enums.SourceType.BUILTIN, null,
                "Read file", "{\"type\":\"object\",\"properties\":{\"file_path\":{\"type\":\"string\"}}}"));
    }

    @Nested
    class ResolveAgentToolsMcp {

        @Test
        void resolvesMcpBindingWithSnapshot() {
            UUID mcpId = UUID.randomUUID();
            // Pre-register MCP tools
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "list_files"), "list_files",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "List files", "{\"type\":\"object\"}"));

            McpEntity enabledMcp = new McpEntity();
            enabledMcp.setEnabled(true);
            when(mcpMapper.selectById(mcpId)).thenReturn(enabledMcp);

            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "list_files");
            binding.put("source_type", "mcp");
            binding.put("source_id", mcpId.toString());

            List<Map<String, Object>> resolved = registry.resolveAgentTools(List.of(binding), UUID.randomUUID());

            assertThat(resolved).hasSize(1);
            assertThat(resolved.get(0)).containsKeys("source_type", "tool_name", "tool_schema_snapshot");
            assertThat(resolved.get(0).get("source_type")).isEqualTo("mcp");
            assertThat(resolved.get(0).get("tool_schema_snapshot")).asString().contains("\"type\"");
        }

        @Test
        void throwsForDisabledMcp() {
            UUID mcpId = UUID.randomUUID();
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "disabled_tool"), "disabled_tool",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "Disabled tool", "{}"));

            McpEntity disabledMcp = new McpEntity();
            disabledMcp.setEnabled(false);
            when(mcpMapper.selectById(mcpId)).thenReturn(disabledMcp);

            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "disabled_tool");
            binding.put("source_type", "mcp");
            binding.put("source_id", mcpId.toString());

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MCP_CONNECTION_FAILED);
        }

        @Test
        void throwsForNullMcpEntity() {
            UUID mcpId = UUID.randomUUID();
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "orphan_tool"), "orphan_tool",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "Orphan tool", "{}"));

            when(mcpMapper.selectById(mcpId)).thenReturn(null);

            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "orphan_tool");
            binding.put("source_type", "mcp");
            binding.put("source_id", mcpId.toString());

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MCP_CONNECTION_FAILED);
        }

        @Test
        void throwsForMcpBindingWithNullSourceId() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "some_tool");
            binding.put("source_type", "mcp");
            binding.put("source_id", null);

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        void throwsForKnowledgeBindingWithNullSourceId() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "kb_search");
            binding.put("source_type", "knowledge");
            binding.put("source_id", null);

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        void throwsForUnknownSourceType() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "some_tool");
            binding.put("source_type", "invalid_source");
            binding.put("source_id", UUID.randomUUID().toString());

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    class RefreshMcpTools {

        @Test
        void registersToolsFromDiscoveredJson() {
            UUID mcpId = UUID.randomUUID();
            String toolsDiscovered = """
                    [{"name":"tool1","description":"desc1","parameters":{"type":"object"}},
                     {"name":"tool2","description":"desc2","parameters":{"type":"object"}}]""";

            McpEntity mcp = new McpEntity();
            mcp.setEnabled(true);
            mcp.setToolsDiscovered(toolsDiscovered);
            when(mcpMapper.selectById(mcpId)).thenReturn(mcp);

            registry.refreshMcpTools(mcpId);

            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "tool1"))).isPresent();
            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "tool2"))).isPresent();
        }

        @Test
        void removesToolsWhenMcpDisabled() {
            UUID mcpId = UUID.randomUUID();
            // Pre-register a tool
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "old_tool"), "old_tool",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "Old tool", "{}"));

            McpEntity disabledMcp = new McpEntity();
            disabledMcp.setEnabled(false);
            when(mcpMapper.selectById(mcpId)).thenReturn(disabledMcp);

            registry.refreshMcpTools(mcpId);

            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "old_tool"))).isEmpty();
        }

        @Test
        void removesToolsWhenMcpNotFound() {
            UUID mcpId = UUID.randomUUID();
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "orphan_tool"), "orphan_tool",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "Orphan tool", "{}"));

            when(mcpMapper.selectById(mcpId)).thenReturn(null);

            registry.refreshMcpTools(mcpId);

            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "orphan_tool"))).isEmpty();
        }

        @Test
        void handlesMalformedJsonGracefully() {
            UUID mcpId = UUID.randomUUID();
            registry.registerBuiltinTool(new ToolDefinition(
                    ToolDefinition.mcpId(mcpId, "pre_existing"), "pre_existing",
                    com.agentplatform.common.core.enums.SourceType.MCP, mcpId,
                    "Pre-existing", "{}"));

            McpEntity mcp = new McpEntity();
            mcp.setEnabled(true);
            mcp.setToolsDiscovered("not valid json {{{");
            when(mcpMapper.selectById(mcpId)).thenReturn(mcp);

            // Should not throw — malformed JSON is caught and logged
            registry.refreshMcpTools(mcpId);

            // Old tools are removed even on parse failure
            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "pre_existing"))).isEmpty();
        }
    }

    @Nested
    class ResolveAgentToolsBuiltin {

        @Test
        void resolvesBuiltinBinding() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "web_search");
            binding.put("source_type", "builtin");
            binding.put("source_id", null);

            List<Map<String, Object>> resolved = registry.resolveAgentTools(List.of(binding), UUID.randomUUID());

            assertThat(resolved).hasSize(1);
            assertThat(resolved.get(0).get("tool_name")).isEqualTo("web_search");
            assertThat(resolved.get(0).get("tool_schema_snapshot")).asString().contains("\"query\"");
        }

        @Test
        void throwsForUnknownBuiltinTool() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "nonexistent");
            binding.put("source_type", "builtin");
            binding.put("source_id", null);

            assertThatThrownBy(() -> registry.resolveAgentTools(List.of(binding), UUID.randomUUID()))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }
}
