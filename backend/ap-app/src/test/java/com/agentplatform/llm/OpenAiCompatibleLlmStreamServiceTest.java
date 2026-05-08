package com.agentplatform.llm;

import com.agentplatform.chat.llm.LlmMessage;
import com.agentplatform.chat.llm.LlmMessage.LlmToolCall;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("OpenAI-compatible LLM request payload")
class OpenAiCompatibleLlmStreamServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("assistant tool-call messages include DeepSeek reasoning content")
    void toolCallMessagesIncludeReasoningContent() throws Exception {
        String body = buildRequestBody(List.of(
                LlmMessage.assistantToolCalls(
                        "Checking that with a tool.",
                        List.of(new LlmToolCall("call_1", "question", "{\"question\":\"Q?\"}")),
                        "I need one more user answer before continuing.")));

        JsonNode message = objectMapper.readTree(body).path("messages").get(0);

        assertThat(message.path("reasoning_content").asText())
                .isEqualTo("I need one more user answer before continuing.");
        assertThat(message.path("tool_calls").get(0).path("function").path("name").asText())
                .isEqualTo("question");
    }

    @Test
    @DisplayName("blank reasoning content is omitted")
    void blankReasoningContentIsOmitted() throws Exception {
        String body = buildRequestBody(List.of(
                LlmMessage.assistantToolCalls(
                        "Checking that with a tool.",
                        List.of(new LlmToolCall("call_1", "question", "{}")),
                        "   ")));

        JsonNode message = objectMapper.readTree(body).path("messages").get(0);

        assertThat(message.has("reasoning_content")).isFalse();
    }

    private String buildRequestBody(List<LlmMessage> messages) throws Exception {
        OpenAiCompatibleLlmStreamService service = new OpenAiCompatibleLlmStreamService(
                mock(ModelRegistry.class), mock(CredentialStore.class), objectMapper);
        Method method = OpenAiCompatibleLlmStreamService.class.getDeclaredMethod(
                "buildRequestBody", String.class, List.class, List.class);
        method.setAccessible(true);
        return (String) method.invoke(service, "deepseek-v4-flash", messages, List.of());
    }
}
