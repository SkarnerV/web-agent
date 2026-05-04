package com.agentplatform.chat.tool;

import com.agentplatform.common.core.tool.McpToolInvoker;
import com.agentplatform.common.core.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MCP Tool Call — Task 7.3")
class McpToolCallTest {

    @Mock private McpToolInvoker mcpToolInvoker;

    private ToolDispatcher dispatcher;
    private static final UUID MCP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        dispatcher = new ToolDispatcher(mcpToolInvoker);
    }

    @Nested
    @DisplayName("Successful tool execution")
    class SuccessTests {

        @Test
        @DisplayName("delegates to McpToolInvoker with correct parameters")
        void delegatesWithCorrectParams() {
            Map<String, Object> bindings = Map.of(
                    "source_type", "mcp",
                    "source_id", MCP_ID.toString());
            when(mcpToolInvoker.executeTool(eq(MCP_ID), eq("list_files"), eq("{\"path\":\"/\"}")))
                    .thenReturn(ToolResult.success("tc_001", "file1.txt, file2.txt", 120));

            ToolResult result = dispatcher.dispatch("tc_001", "list_files",
                    "{\"path\":\"/\"}", bindings);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.content()).isEqualTo("file1.txt, file2.txt");
            verify(mcpToolInvoker).executeTool(MCP_ID, "list_files", "{\"path\":\"/\"}");
        }

        @Test
        @DisplayName("handles UUID source_id passed as UUID object")
        void handlesUuidObject() {
            Map<String, Object> bindings = Map.of(
                    "source_type", "mcp",
                    "source_id", MCP_ID);
            when(mcpToolInvoker.executeTool(any(), anyString(), anyString()))
                    .thenReturn(ToolResult.success("tc_002", "ok", 50));

            ToolResult result = dispatcher.dispatch("tc_002", "echo", "{}", bindings);

            assertThat(result.isSuccess()).isTrue();
            verify(mcpToolInvoker).executeTool(eq(MCP_ID), eq("echo"), anyString());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorTests {

        @Test
        @DisplayName("returns error when McpToolInvoker returns error (first attempt)")
        void invokerReturnsError() {
            Map<String, Object> bindings = Map.of(
                    "source_type", "mcp",
                    "source_id", MCP_ID.toString());
            when(mcpToolInvoker.executeTool(any(), anyString(), anyString()))
                    .thenReturn(ToolResult.error("tc_003", "Connection refused", 30000));

            ToolResult result = dispatcher.dispatch("tc_003", "remote_tool", "{}", bindings);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.content()).isEqualTo("Connection refused");
        }

        @Test
        @DisplayName("returns error for null source_id")
        void nullSourceId() {
            Map<String, Object> bindings = Map.of("source_type", "mcp");

            ToolResult result = dispatcher.dispatch("tc_004", "tool", "{}", bindings);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.content()).contains("missing source_id");
            verifyNoInteractions(mcpToolInvoker);
        }

        @Test
        @DisplayName("returns error for non-UUID source_id")
        void nonUuidSourceId() {
            Map<String, Object> bindings = Map.of(
                    "source_type", "mcp",
                    "source_id", "garbage");

            ToolResult result = dispatcher.dispatch("tc_005", "tool", "{}", bindings);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.content()).contains("Invalid MCP source_id");
            verifyNoInteractions(mcpToolInvoker);
        }

        @Test
        @DisplayName("unexpected exception is caught and returned as error")
        void unexpectedException() {
            Map<String, Object> bindings = Map.of(
                    "source_type", "mcp",
                    "source_id", MCP_ID.toString());
            when(mcpToolInvoker.executeTool(any(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Unexpected failure"));

            ToolResult result = dispatcher.dispatch("tc_006", "risky_tool", "{}", bindings);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.content()).contains("Unexpected failure");
        }
    }
}
