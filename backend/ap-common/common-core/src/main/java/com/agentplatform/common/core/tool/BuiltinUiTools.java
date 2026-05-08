package com.agentplatform.common.core.tool;

import com.agentplatform.common.core.enums.SourceType;

import java.util.List;
import java.util.Map;

/**
 * Built-in UI tools that are always available to every Agent run.
 *
 * <p>These tools are conversation protocol capabilities, not user-managed
 * external integrations. They should be injected by the chat orchestrator even
 * when an Agent has no explicit tool bindings.</p>
 */
public final class BuiltinUiTools {

    public static final String QUESTION = "question";
    public static final String TODO = "todo";

    private BuiltinUiTools() {
    }

    public static boolean isUiTool(String toolName) {
        return QUESTION.equals(toolName) || TODO.equals(toolName);
    }

    public static ToolDefinition questionDefinition(String parametersJson) {
        return new ToolDefinition(
                ToolDefinition.builtinId(QUESTION),
                QUESTION,
                SourceType.BUILTIN,
                null,
                "Ask the user one question and pause the agent run until the user answers.",
                parametersJson);
    }

    public static ToolDefinition todoDefinition(String parametersJson) {
        return new ToolDefinition(
                ToolDefinition.builtinId(TODO),
                TODO,
                SourceType.BUILTIN,
                null,
                "Create or update the visible todo list for the current agent run.",
                parametersJson);
    }

    public static Map<String, Object> questionSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "question", Map.of(
                                "type", "string",
                                "description", "The question shown to the user"),
                        "options", Map.of(
                                "type", "array",
                                "description", "Answer options. Use an empty array for open-ended questions when allow_free_text is true.",
                                "minItems", 0,
                                "maxItems", 6,
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "id", Map.of("type", "string"),
                                                "label", Map.of("type", "string"),
                                                "description", Map.of("type", "string")),
                                        "required", List.of("id", "label"))),
                        "allow_free_text", Map.of(
                                "type", "boolean",
                                "default", false),
                        "multi_select", Map.of(
                                "type", "boolean",
                                "default", false)),
                "required", List.of("question"));
    }

    public static Map<String, Object> todoSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of(
                                "type", "string",
                                "description", "Todo list title shown in the right context panel"),
                        "items", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "id", Map.of(
                                                        "type", "string",
                                                        "description", "Stable item id. Reuse it when updating an existing item."),
                                                "title", Map.of("type", "string"),
                                                "status", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("pending", "in_progress", "completed", "blocked")),
                                                "detail", Map.of("type", "string")),
                                        "required", List.of("id", "title", "status")))),
                "required", List.of("items"));
    }
}
