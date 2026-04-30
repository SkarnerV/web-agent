package com.agentplatform.chat.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds SSE events per §3.8.1 specification.
 * Each instance is bound to a single request/message lifecycle.
 */
public class SseEventBuilder {

    private final String requestId;
    private final String messageId;
    private final ObjectMapper objectMapper;
    private final AtomicLong eventCounter = new AtomicLong(0);
    private final AtomicInteger seqCounter = new AtomicInteger(0);

    public SseEventBuilder(String requestId, String messageId, ObjectMapper objectMapper) {
        this.requestId = requestId;
        this.messageId = messageId;
        this.objectMapper = objectMapper;
    }

    public static String generateEventId() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public String nextEventId() {
        return String.format("evt_%05d", eventCounter.incrementAndGet());
    }

    public SseEmitter.SseEventBuilder messageStart(UUID agentId, String model) {
        Map<String, Object> data = baseData();
        data.put("agent_id", agentId.toString());
        data.put("model", model);
        return buildEvent("message_start", data);
    }

    public SseEmitter.SseEventBuilder token(String delta) {
        Map<String, Object> data = baseData();
        data.put("delta", delta);
        data.put("seq", seqCounter.incrementAndGet());
        return buildEvent("token", data);
    }

    public SseEmitter.SseEventBuilder toolCallStart(String toolCallId, String toolName,
                                                     String arguments, int stepNumber) {
        Map<String, Object> data = baseData();
        data.put("tool_call_id", toolCallId);
        data.put("tool_name", toolName);
        data.put("arguments", arguments);
        data.put("step_number", stepNumber);
        return buildEvent("tool_call_start", data);
    }

    public SseEmitter.SseEventBuilder toolCallEnd(String toolCallId, String status,
                                                   String resultSummary, long durationMs) {
        Map<String, Object> data = baseData();
        data.put("tool_call_id", toolCallId);
        data.put("status", status);
        data.put("result_summary", resultSummary);
        data.put("duration_ms", durationMs);
        return buildEvent("tool_call_end", data);
    }

    public SseEmitter.SseEventBuilder citation(List<Map<String, Object>> sources) {
        Map<String, Object> data = baseData();
        data.put("sources", sources);
        return buildEvent("citation", data);
    }

    public SseEmitter.SseEventBuilder stepLimit(int currentStep, int maxSteps, UUID sessionStateId) {
        Map<String, Object> data = baseData();
        data.put("current_step", currentStep);
        data.put("max_steps", maxSteps);
        data.put("session_state_id", sessionStateId.toString());
        return buildEvent("step_limit", data);
    }

    public SseEmitter.SseEventBuilder messageEnd(String finishReason, Map<String, Object> usage, int totalSteps) {
        Map<String, Object> data = baseData();
        data.put("finish_reason", finishReason);
        data.put("usage", usage);
        data.put("total_steps", totalSteps);
        return buildEvent("message_end", data);
    }

    public SseEmitter.SseEventBuilder error(String code, String message, boolean recoverable) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        data.put("recoverable", recoverable);
        data.put("request_id", requestId);
        return buildEvent("error", data);
    }

    public SseEmitter.SseEventBuilder heartbeat() {
        return buildEvent("heartbeat", Map.of());
    }

    public int getCurrentSeq() {
        return seqCounter.get();
    }

    private Map<String, Object> baseData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("request_id", requestId);
        data.put("message_id", messageId);
        data.put("timestamp", OffsetDateTime.now().toString());
        return data;
    }

    private SseEmitter.SseEventBuilder buildEvent(String eventType, Map<String, Object> data) {
        String id = nextEventId();
        try {
            String json = objectMapper.writeValueAsString(data);
            return SseEmitter.event()
                    .id(id)
                    .name(eventType)
                    .data(json);
        } catch (JsonProcessingException e) {
            return SseEmitter.event()
                    .id(id)
                    .name("error")
                    .data("{\"code\":\"INTERNAL_ERROR\",\"message\":\"JSON serialization failed\"}");
        }
    }
}
