package com.agentplatform.common.core.tool;

import com.agentplatform.common.core.enums.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    private StubToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StubToolRegistry();

        // Register builtin tools (simulating startup scan)
        registry.registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("web_search"), "web_search",
                SourceType.BUILTIN, null,
                "Search the web", "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}"));
        registry.registerBuiltinTool(new ToolDefinition(
                ToolDefinition.builtinId("calculator"), "calculator",
                SourceType.BUILTIN, null,
                "Evaluate math", "{\"type\":\"object\",\"properties\":{\"expression\":{\"type\":\"string\"}}}"));

        // Register a MCP tool (simulating tools_discovered load)
        UUID mcpId = UUID.randomUUID();
        registry.tools.put(ToolDefinition.mcpId(mcpId, "list_files"),
                new ToolDefinition(ToolDefinition.mcpId(mcpId, "list_files"), "list_files",
                        SourceType.MCP, mcpId,
                        "List files", "{\"type\":\"object\"}"));

        // Register a knowledge search tool
        UUID kbId = UUID.randomUUID();
        registry.tools.put(ToolDefinition.knowledgeId(kbId),
                new ToolDefinition(ToolDefinition.knowledgeId(kbId), "kb_search",
                        SourceType.KNOWLEDGE, kbId,
                        "Search knowledge base", "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}"));
    }

    @Nested
    class ToolDefinitionIds {

        @Test
        void builtinId() {
            assertThat(ToolDefinition.builtinId("web_search")).isEqualTo("builtin_web_search");
            assertThat(ToolDefinition.builtinId("calculator")).startsWith("builtin_");
        }

        @Test
        void mcpId() {
            UUID mcpId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            String id = ToolDefinition.mcpId(mcpId, "list_files");
            assertThat(id).startsWith("mcp_").endsWith("_list_files").contains(mcpId.toString());
        }

        @Test
        void knowledgeId() {
            UUID kbId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            assertThat(ToolDefinition.knowledgeId(kbId)).isEqualTo("knowledge_22222222-2222-2222-2222-222222222222");
        }
    }

    @Nested
    class ThreeSourceTypes {

        @Test
        void builtinToolsAreAvailable() {
            List<ToolDefinition> tools = registry.getAvailableTools(UUID.randomUUID());
            assertThat(tools).extracting(ToolDefinition::getSourceType)
                    .contains(SourceType.BUILTIN);
            assertThat(tools).extracting(ToolDefinition::getToolName)
                    .contains("web_search", "calculator");
        }

        @Test
        void mcpToolsAreAvailable() {
            List<ToolDefinition> tools = registry.getAvailableTools(UUID.randomUUID());
            assertThat(tools).extracting(ToolDefinition::getSourceType)
                    .contains(SourceType.MCP);
        }

        @Test
        void knowledgeToolsAreAvailable() {
            List<ToolDefinition> tools = registry.getAvailableTools(UUID.randomUUID());
            assertThat(tools).extracting(ToolDefinition::getSourceType)
                    .contains(SourceType.KNOWLEDGE);
        }
    }

    @Nested
    class GetToolById {

        @Test
        void findsBuiltinTool() {
            Optional<ToolDefinition> def = registry.getToolById("builtin_web_search");
            assertThat(def).isPresent();
            assertThat(def.get().getToolName()).isEqualTo("web_search");
            assertThat(def.get().getSourceType()).isEqualTo(SourceType.BUILTIN);
        }

        @Test
        void returnsEmptyForUnknownTool() {
            assertThat(registry.getToolById("nonexistent")).isEmpty();
        }
    }

    @Nested
    class ResolveAgentTools {

        @Test
        void resolvesValidBindingWithSnapshot() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "web_search");
            binding.put("source_type", "builtin");
            binding.put("source_id", null);
            List<Map<String, Object>> bindings = List.of(binding);

            List<Map<String, Object>> resolved = registry.resolveAgentTools(bindings, UUID.randomUUID());
            assertThat(resolved).hasSize(1);
            assertThat(resolved.get(0)).containsKeys("source_type", "tool_name", "tool_schema_snapshot");
            assertThat(resolved.get(0).get("tool_schema_snapshot")).asString().contains("\"query\"");
        }

        @Test
        void throwsForInvalidTool() {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("tool_name", "nonexistent_tool");
            binding.put("source_type", "builtin");
            binding.put("source_id", null);
            List<Map<String, Object>> bindings = List.of(binding);

            assertThatThrownBy(() -> registry.resolveAgentTools(bindings, UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void throwsForDisabledMCP() {
            UUID mcpId = UUID.randomUUID();
            // Register a tool from a disabled MCP
            registry.tools.put(ToolDefinition.mcpId(mcpId, "disabled_tool"),
                    new ToolDefinition(ToolDefinition.mcpId(mcpId, "disabled_tool"), "disabled_tool",
                            SourceType.MCP, mcpId, "Disabled tool", "{}"));
            // Simulate MCP being disabled
            registry.disabledMcpIds.add(mcpId);

            List<Map<String, Object>> bindings = List.of(Map.of(
                    "tool_name", "disabled_tool",
                    "source_type", "mcp",
                    "source_id", mcpId.toString()));

            assertThatThrownBy(() -> registry.resolveAgentTools(bindings, UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        void emptyBindingsReturnsEmptyList() {
            assertThat(registry.resolveAgentTools(List.of(), UUID.randomUUID())).isEmpty();
            assertThat(registry.resolveAgentTools(null, UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    class RegisterBuiltinTool {

        @Test
        void registersAndEnables() {
            ToolDefinition def = new ToolDefinition("builtin_new_tool", "new_tool",
                    SourceType.BUILTIN, null, "A new tool", "{}");
            def.setEnabled(false);
            registry.registerBuiltinTool(def);
            assertThat(registry.getToolById("builtin_new_tool")).isPresent()
                    .get().extracting(ToolDefinition::isEnabled).isEqualTo(true);
        }
    }

    // ─── RefreshMcpTools ───

    @Nested
    class RefreshMcpTools {

        @Test
        void refreshRegistersNewToolsAndRemovesOld() {
            UUID mcpId = UUID.randomUUID();
            // Pre-register an old tool from this MCP
            String oldToolId = ToolDefinition.mcpId(mcpId, "old_tool");
            registry.tools.put(oldToolId,
                    new ToolDefinition(oldToolId, "old_tool",
                            SourceType.MCP, mcpId, "Old tool", "{}"));

            // Set up the MCP tool source with new tools
            registry.mcpToolSource.put(mcpId, List.of(
                    new ToolDefinition(ToolDefinition.mcpId(mcpId, "new_tool_1"), "new_tool_1",
                            SourceType.MCP, mcpId, "New tool 1", "{\"type\":\"object\"}"),
                    new ToolDefinition(ToolDefinition.mcpId(mcpId, "new_tool_2"), "new_tool_2",
                            SourceType.MCP, mcpId, "New tool 2", "{\"type\":\"object\"}")));

            registry.refreshMcpTools(mcpId);

            assertThat(registry.getToolById(oldToolId)).isEmpty();
            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "new_tool_1"))).isPresent();
            assertThat(registry.getToolById(ToolDefinition.mcpId(mcpId, "new_tool_2"))).isPresent();
            List<ToolDefinition> available = registry.getAvailableTools(UUID.randomUUID());
            assertThat(available).extracting(ToolDefinition::getToolName)
                    .contains("new_tool_1", "new_tool_2")
                    .doesNotContain("old_tool");
        }

        @Test
        void refreshForDisabledOrNonexistentMcpRemovesTools() {
            UUID mcpId = UUID.randomUUID();
            String toolId = ToolDefinition.mcpId(mcpId, "some_tool");
            registry.tools.put(toolId,
                    new ToolDefinition(toolId, "some_tool",
                            SourceType.MCP, mcpId, "Some tool", "{}"));

            registry.refreshMcpTools(mcpId);

            assertThat(registry.getToolById(toolId)).isEmpty();
            List<ToolDefinition> available = registry.getAvailableTools(UUID.randomUUID());
            assertThat(available).extracting(ToolDefinition::getToolName)
                    .doesNotContain("some_tool");
        }
    }

    // ─── Stub implementation for interface contract tests ───

    static class StubToolRegistry implements ToolRegistry {
        final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
        final Set<UUID> disabledMcpIds = new HashSet<>();
        final Map<UUID, List<ToolDefinition>> mcpToolSource = new HashMap<>();

        @Override
        public List<ToolDefinition> getAvailableTools(UUID userId) {
            return tools.values().stream()
                    .filter(ToolDefinition::isEnabled)
                    .toList();
        }

        @Override
        public Optional<ToolDefinition> getToolById(String toolId) {
            return Optional.ofNullable(tools.get(toolId));
        }

        @Override
        public List<Map<String, Object>> resolveAgentTools(List<Map<String, Object>> bindings, UUID userId) {
            if (bindings == null || bindings.isEmpty()) return List.of();
            List<Map<String, Object>> resolved = new ArrayList<>();
            for (Map<String, Object> b : bindings) {
                String toolName = (String) b.get("tool_name");
                String sourceType = (String) b.get("source_type");
                Object sid = b.get("source_id");

                UUID sourceId = null;
                if (sid instanceof UUID uuid) sourceId = uuid;
                else if (sid != null) {
                    try { sourceId = UUID.fromString(sid.toString()); } catch (IllegalArgumentException ignored) {}
                }

                ToolDefinition def = null;
                if ("builtin".equals(sourceType)) {
                    def = tools.get(ToolDefinition.builtinId(toolName));
                } else if ("mcp".equals(sourceType)) {
                    if (disabledMcpIds.contains(sourceId)) {
                        throw new RuntimeException("MCP is disabled");
                    }
                    def = tools.get(ToolDefinition.mcpId(sourceId, toolName));
                } else if ("knowledge".equals(sourceType)) {
                    def = tools.get(ToolDefinition.knowledgeId(sourceId));
                }

                if (def == null || !def.isEnabled()) {
                    throw new RuntimeException("Tool not found: " + toolName);
                }

                Map<String, Object> snapshot = new LinkedHashMap<>();
                snapshot.put("source_type", sourceType);
                snapshot.put("source_id", sourceId != null ? sourceId.toString() : null);
                snapshot.put("tool_name", toolName);
                snapshot.put("tool_schema_snapshot", def.getParameters());
                resolved.add(snapshot);
            }
            return resolved;
        }

        @Override
        public void refreshMcpTools(UUID mcpId) {
            tools.entrySet().removeIf(e ->
                    e.getValue().getSourceType() == SourceType.MCP && mcpId.equals(e.getValue().getSourceId()));
            List<ToolDefinition> newTools = mcpToolSource.get(mcpId);
            if (newTools != null) {
                for (ToolDefinition def : newTools) {
                    def.setEnabled(true);
                    tools.put(def.getToolId(), def);
                }
            }
        }

        @Override
        public void registerBuiltinTool(ToolDefinition def) {
            def.setEnabled(true);
            tools.put(def.getToolId(), def);
        }
    }
}
