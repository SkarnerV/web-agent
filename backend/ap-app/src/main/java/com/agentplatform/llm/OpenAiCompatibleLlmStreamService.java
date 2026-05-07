package com.agentplatform.llm;

import com.agentplatform.chat.llm.LlmChunk;
import com.agentplatform.chat.llm.LlmMessage;
import com.agentplatform.chat.llm.LlmStreamService;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Real LLM streaming implementation using OpenAI-compatible chat/completions API.
 * Supports both custom models (user-provided key) and builtin models (platform key).
 */
@Service
@Primary
public class OpenAiCompatibleLlmStreamService implements LlmStreamService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmStreamService.class);

    private final ModelRegistry modelRegistry;
    private final CredentialStore credentialStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${agent-platform.builtin-model-key:}")
    private String builtinModelKey;

    public OpenAiCompatibleLlmStreamService(ModelRegistry modelRegistry,
                                            CredentialStore credentialStore,
                                            ObjectMapper objectMapper) {
        this.modelRegistry = modelRegistry;
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Iterator<LlmChunk> stream(String modelId, List<LlmMessage> messages, List<Map<String, Object>> tools) {
        if (modelId == null || modelId.isBlank()) {
            throw new BizException(ErrorCode.CHAT_MODEL_ERROR, Map.of("reason", "model_id is null"));
        }

        ModelInfo model = modelRegistry.getById(modelId)
                .orElseThrow(() -> new BizException(ErrorCode.CHAT_MODEL_ERROR,
                        Map.of("reason", "Model not found: " + modelId)));

        if (model.getSource() == ModelInfo.Source.BUILTIN
                && (builtinModelKey == null || builtinModelKey.isBlank())) {
            log.info("No builtin model API key configured; using local development LLM stub for {}", modelId);
            return localStubStream();
        }

        String apiUrl = resolveApiUrl(model);
        String apiKey = resolveApiKey(model);
        String modelName = resolveModelName(model);

        String endpoint = apiUrl.endsWith("/")
                ? apiUrl + "chat/completions"
                : apiUrl + "/chat/completions";

        log.debug("LLM stream: model={}, endpoint={}, messages={}", modelName, endpoint, messages.size());

        try {
            String requestBody = buildRequestBody(modelName, messages, tools);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMinutes(3))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("LLM API error: status={}, body={}", response.statusCode(), errorBody);
                throw new BizException(ErrorCode.CHAT_MODEL_ERROR,
                        Map.of("reason", "API returned " + response.statusCode(), "detail", errorBody));
            }

            return new SseChunkIterator(response.body());

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM stream failed: {}", e.getMessage(), e);
            throw new BizException(ErrorCode.CHAT_MODEL_ERROR,
                    Map.of("reason", e.getMessage()));
        }
    }

    private String resolveApiUrl(ModelInfo model) {
        if (model.getSource() == ModelInfo.Source.CUSTOM) {
            return model.getApiUrl();
        }
        // Builtin: get from config map
        Map<String, Object> config = model.getConfig();
        if (config != null && config.containsKey("api_url")) {
            return config.get("api_url").toString();
        }
        throw new BizException(ErrorCode.CHAT_MODEL_ERROR,
                Map.of("reason", "No API URL configured for builtin model: " + model.getId()));
    }

    private String resolveApiKey(ModelInfo model) {
        if (model.getSource() == ModelInfo.Source.CUSTOM) {
            UUID customId = UUID.fromString(model.getId());
            String encrypted = modelRegistry.getRawApiKeyEnc(customId);
            return credentialStore.decrypt(encrypted);
        }
        // Builtin: use platform key
        if (builtinModelKey != null && !builtinModelKey.isBlank()) {
            return builtinModelKey;
        }
        throw new BizException(ErrorCode.CHAT_MODEL_ERROR,
                Map.of("reason", "No platform API key configured for builtin models"));
    }

    private String resolveModelName(ModelInfo model) {
        // For custom models, use the model name as-is (user sets it to the model identifier)
        // For builtin, use the ID (e.g. "gpt-4o")
        if (model.getSource() == ModelInfo.Source.BUILTIN) {
            return model.getId();
        }
        return model.getName();
    }

    private Iterator<LlmChunk> localStubStream() {
        List<LlmChunk> chunks = new ArrayList<>();
        chunks.add(new LlmChunk.TokenChunk("我是"));
        chunks.add(new LlmChunk.TokenChunk("本地"));
        chunks.add(new LlmChunk.TokenChunk("开发"));
        chunks.add(new LlmChunk.TokenChunk("模型"));
        chunks.add(new LlmChunk.TokenChunk("助手"));
        chunks.add(new LlmChunk.TokenChunk("，"));
        chunks.add(new LlmChunk.TokenChunk("已收到"));
        chunks.add(new LlmChunk.TokenChunk("你的消息。"));
        chunks.add(new LlmChunk.FinishChunk("stop", 50, 20));
        return chunks.iterator();
    }

    private String buildRequestBody(String modelName, List<LlmMessage> messages,
                                    List<Map<String, Object>> tools) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("stream", true);

        List<Map<String, Object>> msgList = new ArrayList<>();
        for (LlmMessage msg : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.role());
            m.put("content", msg.content());
            if ("assistant".equals(msg.role()) && msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (LlmMessage.LlmToolCall toolCall : msg.toolCalls()) {
                    toolCalls.add(Map.of(
                            "id", toolCall.id(),
                            "type", "function",
                            "function", Map.of(
                                    "name", toolCall.name(),
                                    "arguments", toolCall.arguments() != null ? toolCall.arguments() : "{}")));
                }
                m.put("tool_calls", toolCalls);
            }
            if ("tool".equals(msg.role()) && msg.toolResult() != null) {
                m.put("tool_call_id", msg.toolResult().get("tool_call_id"));
            }
            msgList.add(m);
        }
        body.put("messages", msgList);

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        return objectMapper.writeValueAsString(body);
    }

    /**
     * Parses Server-Sent Events from the response InputStream into LlmChunk items.
     */
    private class SseChunkIterator implements Iterator<LlmChunk> {

        private final BufferedReader reader;
        private final Queue<LlmChunk> buffer = new ArrayDeque<>();
        private final Map<Integer, ToolCallAccumulator> pendingToolCalls = new LinkedHashMap<>();
        private boolean done = false;
        private int promptTokens = 0;
        private int completionTokens = 0;

        SseChunkIterator(java.io.InputStream inputStream) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }

        @Override
        public boolean hasNext() {
            if (!buffer.isEmpty()) return true;
            if (done) return false;
            fillBuffer();
            return !buffer.isEmpty();
        }

        @Override
        public LlmChunk next() {
            if (!hasNext()) throw new NoSuchElementException();
            return buffer.poll();
        }

        private void fillBuffer() {
            try {
                while (buffer.isEmpty() && !done) {
                    String line = reader.readLine();
                    if (line == null) {
                        finishStream("stop");
                        return;
                    }
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            finishStream("stop");
                            return;
                        }
                        parseDataChunk(data);
                    }
                }
            } catch (Exception e) {
                log.error("Error reading SSE stream: {}", e.getMessage());
                buffer.add(new LlmChunk.ErrorChunk("STREAM_ERROR", e.getMessage()));
                done = true;
            }
        }

        private void parseDataChunk(String json) {
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode choices = root.get("choices");
                if (choices == null || choices.isEmpty()) {
                    // Usage-only chunk (some APIs send usage in a separate final chunk)
                    JsonNode usage = root.get("usage");
                    if (usage != null) {
                        promptTokens = usage.path("prompt_tokens").asInt(0);
                        completionTokens = usage.path("completion_tokens").asInt(0);
                    }
                    return;
                }

                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");
                if (delta == null) return;

                String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isNull()
                        ? choice.get("finish_reason").asText() : null;

                // Token content
                if (delta.has("content") && !delta.get("content").isNull()) {
                    String content = delta.get("content").asText();
                    if (!content.isEmpty()) {
                        buffer.add(new LlmChunk.TokenChunk(content));
                    }
                }

                // Tool calls
                if (delta.has("tool_calls")) {
                    JsonNode toolCalls = delta.get("tool_calls");
                    for (JsonNode tc : toolCalls) {
                        int index = tc.has("index") ? tc.get("index").asInt() : pendingToolCalls.size();
                        ToolCallAccumulator acc = pendingToolCalls.computeIfAbsent(index, k -> new ToolCallAccumulator());
                        if (tc.has("id") && !tc.get("id").isNull()) {
                            acc.id = tc.get("id").asText();
                        }
                        JsonNode fn = tc.get("function");
                        if (fn != null) {
                            if (fn.has("name") && !fn.get("name").isNull()) {
                                acc.name = fn.get("name").asText();
                            }
                            if (fn.has("arguments") && !fn.get("arguments").isNull()) {
                                acc.arguments.append(fn.get("arguments").asText());
                            }
                        }
                    }
                }

                // Finish
                if (finishReason != null) {
                    JsonNode usage = root.get("usage");
                    if (usage != null) {
                        promptTokens = usage.path("prompt_tokens").asInt(0);
                        completionTokens = usage.path("completion_tokens").asInt(0);
                    }
                    if ("tool_calls".equals(finishReason)) {
                        flushPendingToolCalls();
                    }
                    finishStream(finishReason);
                }
            } catch (Exception e) {
                log.warn("Failed to parse SSE chunk: {}", e.getMessage());
            }
        }

        private void flushPendingToolCalls() {
            for (ToolCallAccumulator acc : pendingToolCalls.values()) {
                if (acc.name != null && !acc.name.isBlank()) {
                    buffer.add(new LlmChunk.ToolCallChunk(
                            acc.id != null ? acc.id : "",
                            acc.name,
                            acc.arguments.isEmpty() ? "{}" : acc.arguments.toString()));
                }
            }
            pendingToolCalls.clear();
        }

        private void finishStream(String reason) {
            if (!done) {
                if (!pendingToolCalls.isEmpty()) {
                    flushPendingToolCalls();
                }
                buffer.add(new LlmChunk.FinishChunk(reason, promptTokens, completionTokens));
                done = true;
                try { reader.close(); } catch (Exception ignored) {}
            }
        }

        private class ToolCallAccumulator {
            private String id;
            private String name;
            private final StringBuilder arguments = new StringBuilder();
        }
    }
}
