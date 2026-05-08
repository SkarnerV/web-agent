package com.agentplatform.chat.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LlmMessage JSON compatibility")
class LlmMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("old context snapshots without reasoningContent still deserialize")
    void oldSnapshotsWithoutReasoningContentDeserialize() throws Exception {
        String json = """
                [{
                  "role": "assistant",
                  "content": "Need one more answer.",
                  "toolCalls": null,
                  "toolResult": null
                }]
                """;

        List<LlmMessage> messages = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).reasoningContent()).isNull();
    }

    @Test
    @DisplayName("reasoningContent survives context snapshot serialization")
    void reasoningContentSurvivesSerialization() throws Exception {
        List<LlmMessage> original = List.of(LlmMessage.assistantToolCalls(
                "Need one more answer.",
                List.of(new LlmMessage.LlmToolCall("call_1", "question", "{}")),
                "The user requested a multi-question loop."));

        String json = objectMapper.writeValueAsString(original);
        List<LlmMessage> restored = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(restored.get(0).reasoningContent())
                .isEqualTo("The user requested a multi-question loop.");
    }
}
