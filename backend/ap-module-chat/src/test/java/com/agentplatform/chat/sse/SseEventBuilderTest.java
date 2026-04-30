package com.agentplatform.chat.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseEventBuilder — §3.8.1 event types")
class SseEventBuilderTest {

    private SseEventBuilder builder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        builder = new SseEventBuilder("req_test123", "msg_test456", objectMapper);
    }

    @Test
    @DisplayName("message_start event is not null")
    void messageStart() {
        UUID agentId = UUID.randomUUID();
        Object event = builder.messageStart(agentId, "gpt-4o");
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("token events have monotonically increasing seq")
    void tokenSeqMonotonicallyIncreasing() {
        for (int i = 1; i <= 100; i++) {
            builder.token("delta-" + i);
            assertThat(builder.getCurrentSeq()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("tool_call_start produces non-null event")
    void toolCallStart() {
        Object event = builder.toolCallStart(
                "tc_001", "web_search", "{\"query\":\"test\"}", 1);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("tool_call_end produces non-null event")
    void toolCallEnd() {
        Object event = builder.toolCallEnd(
                "tc_001", "success", "Found 3 results", 1500);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("citation event is not null")
    void citation() {
        List<Map<String, Object>> sources = List.of(
                Map.of("doc_name", "guide.pdf", "page", 5, "score", 0.92));
        Object event = builder.citation(sources);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("step_limit event is not null")
    void stepLimit() {
        UUID stateId = UUID.randomUUID();
        Object event = builder.stepLimit(10, 10, stateId);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("message_end event is not null")
    void messageEnd() {
        Map<String, Object> usage = Map.of(
                "prompt_tokens", 150,
                "completion_tokens", 80);
        Object event = builder.messageEnd("stop", usage, 3);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("error event is not null")
    void errorEvent() {
        Object event = builder.error(
                "CHAT_MODEL_ERROR", "LLM service unavailable", false);
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("heartbeat event is valid")
    void heartbeat() {
        Object event = builder.heartbeat();
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("event IDs are monotonically incrementing")
    void eventIdsIncrement() {
        String id1 = builder.nextEventId();
        String id2 = builder.nextEventId();
        String id3 = builder.nextEventId();
        assertThat(id1).isEqualTo("evt_00001");
        assertThat(id2).isEqualTo("evt_00002");
        assertThat(id3).isEqualTo("evt_00003");
    }
}
