package com.agentplatform.chat.tool;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.tool.BuiltinUiTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class BuiltinToolExecutor {

    private static final Set<String> TODO_STATUSES = Set.of(
            "pending", "in_progress", "completed", "blocked");

    private final ObjectMapper objectMapper;

    public BuiltinToolExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean supports(String toolName) {
        return BuiltinUiTools.isUiTool(toolName);
    }

    public TodoState applyTodo(String arguments, TodoState current) {
        JsonNode root = parseObject(arguments);
        JsonNode itemsNode = root.get("items");
        if (itemsNode == null || !itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "todo.items must contain at least one item"));
        }

        String title = textOrNull(root.get("title"));
        Map<String, TodoItem> merged = new LinkedHashMap<>();
        if (current != null) {
            title = title != null ? title : current.title();
            for (TodoItem item : current.items()) {
                merged.put(item.id(), item);
            }
        }

        for (JsonNode itemNode : itemsNode) {
            String id = requiredText(itemNode, "id", "todo item id is required");
            String status = requiredText(itemNode, "status", "todo item status is required");
            if (!TODO_STATUSES.contains(status)) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "Unsupported todo status: " + status));
            }

            TodoItem existing = merged.get(id);
            String itemTitle = textOrNull(itemNode.get("title"));
            if ((itemTitle == null || itemTitle.isBlank()) && existing == null) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "todo item title is required for new item: " + id));
            }
            String detail = textOrNull(itemNode.get("detail"));
            merged.put(id, new TodoItem(
                    id,
                    itemTitle != null ? itemTitle : existing.title(),
                    status,
                    detail != null ? detail : existing != null ? existing.detail() : null));
        }

        return new TodoState(title, new ArrayList<>(merged.values()));
    }

    public PendingQuestion parseQuestion(String toolCallId, String arguments) {
        JsonNode root = parseObject(arguments);
        String question = requiredText(root, "question", "question is required");
        boolean allowFreeText = true;
        boolean multiSelect = root.path("multi_select").asBoolean(false);
        JsonNode optionsNode = root.get("options");
        if (optionsNode == null || optionsNode.isNull()) {
            optionsNode = objectMapper.createArrayNode();
        }
        if (!optionsNode.isArray() || optionsNode.size() < 3 || optionsNode.size() > 6) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "question.options must contain 3-6 options"));
        }

        List<QuestionOption> options = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode optionNode : optionsNode) {
            String id = requiredText(optionNode, "id", "question option id is required");
            String label = requiredText(optionNode, "label", "question option label is required");
            if (!ids.add(id)) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "Duplicate question option id: " + id));
            }
            options.add(new QuestionOption(id, label, textOrNull(optionNode.get("description"))));
        }

        return new PendingQuestion(
                "q_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                toolCallId,
                question,
                options,
                allowFreeText,
                multiSelect);
    }

    public String buildQuestionAnswerResult(PendingQuestion pending,
                                            List<String> selectedOptionIds,
                                            String answerText) {
        validateAnswer(pending, selectedOptionIds, answerText);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question_id", pending.questionId());
        result.put("selected_option_ids", selectedOptionIds != null ? selectedOptionIds : List.of());
        result.put("answer_text", answerText);
        result.put("answered_at", OffsetDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }

    public void validateAnswer(PendingQuestion pending,
                               List<String> selectedOptionIds,
                               String answerText) {
        if (pending == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, Map.of("reason", "No pending question"));
        }

        List<String> ids = selectedOptionIds != null ? selectedOptionIds : List.of();
        boolean hasText = answerText != null && !answerText.isBlank();
        if (ids.isEmpty() && (!pending.allowFreeText() || !hasText)) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Question answer is required"));
        }
        if (!pending.multiSelect() && ids.size() > 1) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Question only allows one selected option"));
        }

        Set<String> allowed = new LinkedHashSet<>();
        for (QuestionOption option : pending.options()) {
            allowed.add(option.id());
        }
        for (String id : ids) {
            if (!allowed.contains(id)) {
                throw new BizException(ErrorCode.INVALID_REQUEST,
                        Map.of("reason", "Unknown question option: " + id));
            }
        }
    }

    private JsonNode parseObject(String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Tool arguments must be a JSON object");
            }
            return root;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("reason", "Invalid tool arguments JSON: " + e.getMessage()));
        }
    }

    private String requiredText(JsonNode node, String field, String message) {
        return requiredText(node.get(field), message);
    }

    private String requiredText(JsonNode node, String message) {
        String value = textOrNull(node);
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, Map.of("reason", message));
        }
        return value;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    public record TodoState(String title, List<TodoItem> items) {}

    public record TodoItem(String id, String title, String status, String detail) {}

    public record PendingQuestion(
            String questionId,
            String toolCallId,
            String question,
            List<QuestionOption> options,
            boolean allowFreeText,
            boolean multiSelect) {}

    public record QuestionOption(String id, String label, String description) {}
}
