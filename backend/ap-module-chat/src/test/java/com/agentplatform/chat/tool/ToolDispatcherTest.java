package com.agentplatform.chat.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolDispatcher — 4.5 Tool Routing")
class ToolDispatcherTest {

    private ToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ToolDispatcher();
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
    @DisplayName("routes to MCP when source_type is mcp")
    void routeMcp() {
        Map<String, Object> bindings = Map.of(
                "source_type", "mcp",
                "source_id", "mcp-uuid-123");
        ToolResult result = dispatcher.dispatch("tc_002", "list_buckets", "{}", bindings);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("routes to knowledge when source_type is knowledge")
    void routeKnowledge() {
        Map<String, Object> bindings = Map.of(
                "source_type", "knowledge",
                "source_id", "kb-uuid-456");
        ToolResult result = dispatcher.dispatch("tc_003", "search_kb", "{\"query\":\"test\"}", bindings);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("defaults to builtin when bindings are empty")
    void defaultToBuiltin() {
        ToolResult result = dispatcher.dispatch("tc_004", "unknown_tool", "{}", Map.of());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("returns error result for unknown source type")
    void unknownSourceType() {
        Map<String, Object> bindings = Map.of("source_type", "alien");
        ToolResult result = dispatcher.dispatch("tc_005", "x", "{}", bindings);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    @DisplayName("duration_ms is always non-negative")
    void durationNonNegative() {
        ToolResult result = dispatcher.dispatch("tc_006", "test", "{}", Map.of());
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }
}
