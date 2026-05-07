package com.agentplatform.chat.llm;

import com.agentplatform.common.core.tool.BuiltinUiTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LlmToolSpecFactory {

    private final ObjectMapper objectMapper;

    public LlmToolSpecFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> buildTools(Map<String, Map<String, Object>> agentToolBindings) {
        List<Map<String, Object>> tools = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        tools.add(functionTool(BuiltinUiTools.QUESTION,
                "Ask the user a multiple-choice question and pause until the user answers.",
                BuiltinUiTools.questionSchema()));
        added.add(BuiltinUiTools.QUESTION);

        tools.add(functionTool(BuiltinUiTools.TODO,
                "Create or update the visible todo list for the current agent run.",
                BuiltinUiTools.todoSchema()));
        added.add(BuiltinUiTools.TODO);

        if (agentToolBindings == null || agentToolBindings.isEmpty()) {
            return tools;
        }

        for (Map<String, Object> binding : agentToolBindings.values()) {
            String toolName = stringValue(binding.get("tool_name"));
            if (toolName == null || toolName.isBlank() || added.contains(toolName)) {
                continue;
            }

            Object schemaRaw = binding.get("tool_schema_snapshot");
            Map<String, Object> parameters = parseSchema(schemaRaw);
            tools.add(functionTool(toolName, "Agent tool: " + toolName, parameters));
            added.add(toolName);
        }

        return tools;
    }

    private Map<String, Object> functionTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private Map<String, Object> parseSchema(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, new TypeReference<>() {});
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return objectMapper.readValue(s, new TypeReference<>() {});
            } catch (Exception ignored) {
                return Map.of("type", "object", "properties", Map.of());
            }
        }
        return Map.of("type", "object", "properties", Map.of());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
