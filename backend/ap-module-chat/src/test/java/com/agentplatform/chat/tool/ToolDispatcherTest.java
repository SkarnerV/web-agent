package com.agentplatform.chat.tool;

import com.agentplatform.common.core.tool.McpToolInvoker;
import com.agentplatform.common.core.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolDispatcher — 4.5 Tool Routing")
class ToolDispatcherTest {

    @Mock private McpToolInvoker mcpToolInvoker;

    private ToolDispatcher dispatcher;
    private static final UUID MCP_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        dispatcher = new ToolDispatcher(mcpToolInvoker);
    }

    @Test
    @DisplayName("routes to builtin when source_type is builtin")
    void routeBuiltin() {
        Map<String, Object> bindings = Map.of("source_type", "builtin");
        ToolResult result = dispatcher.dispatch("tc_001", "web_search", "{}", bindings);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.toolCallId()).isEqualTo("tc_001");
    }

    @Test
    @DisplayName("delegates MCP tool calls to McpToolInvoker")
    void routeMcp() {
        UUID mcpId = UUID.randomUUID();
        Map<String, Object> bindings = Map.of(
                "source_type", "mcp",
                "source_id", mcpId.toString());
        when(mcpToolInvoker.executeTool(eq(mcpId), eq("list_buckets"), anyString()))
                .thenReturn(ToolResult.success("tc_002", "buckets: [a, b]", 150));

        ToolResult result = dispatcher.dispatch("tc_002", "list_buckets", "{\"bucket\":\"x\"}", bindings);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.content()).isEqualTo("buckets: [a, b]");
    }

    @Test
    @DisplayName("returns error for MCP binding with missing source_id")
    void routeMcp_missingSourceId() {
        Map<String, Object> bindings = Map.of("source_type", "mcp");
        ToolResult result = dispatcher.dispatch("tc_003", "tool", "{}", bindings);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.content()).contains("missing source_id");
    }

    @Test
    @DisplayName("returns error for MCP binding with invalid source_id")
    void routeMcp_invalidSourceId() {
        Map<String, Object> bindings = Map.of(
                "source_type", "mcp",
                "source_id", "not-a-uuid");
        ToolResult result = dispatcher.dispatch("tc_004", "tool", "{}", bindings);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.content()).contains("Invalid MCP source_id");
    }

    @Test
    @DisplayName("routes to knowledge when source_type is knowledge")
    void routeKnowledge() {
        Map<String, Object> bindings = Map.of(
                "source_type", "knowledge",
                "source_id", UUID.randomUUID().toString());
        ToolResult result = dispatcher.dispatch("tc_005", "search_kb", "{\"query\":\"test\"}", bindings);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("defaults to builtin when bindings are empty")
    void defaultToBuiltin() {
        ToolResult result = dispatcher.dispatch("tc_006", "unknown_tool", "{}", Map.of());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("returns error result for unknown source type")
    void unknownSourceType() {
        Map<String, Object> bindings = Map.of("source_type", "alien");
        ToolResult result = dispatcher.dispatch("tc_007", "x", "{}", bindings);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    @DisplayName("duration_ms is always non-negative")
    void durationNonNegative() {
        ToolResult result = dispatcher.dispatch("tc_008", "test", "{}", Map.of());
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }
}
