package com.agentplatform.chat.llm;

import com.agentplatform.common.core.tool.BuiltinUiTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LlmToolSpecFactory")
class LlmToolSpecFactoryTest {

    @Test
    @DisplayName("question tool advertises open-ended loop usage")
    @SuppressWarnings("unchecked")
    void questionToolAdvertisesOpenEndedLoopUsage() {
        LlmToolSpecFactory factory = new LlmToolSpecFactory(new ObjectMapper());

        Map<String, Object> questionTool = factory.buildTools(Map.of()).stream()
                .filter(tool -> {
                    Map<String, Object> function = (Map<String, Object>) tool.get("function");
                    return BuiltinUiTools.QUESTION.equals(function.get("name"));
                })
                .findFirst()
                .orElseThrow();

        Map<String, Object> function = (Map<String, Object>) questionTool.get("function");
        String description = (String) function.get("description");
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        Map<String, Object> options = (Map<String, Object>) properties.get("options");

        assertThat(description).contains("options=[]", "multi-question loops");
        assertThat((List<String>) parameters.get("required")).containsExactly("question");
        assertThat(options.get("minItems")).isEqualTo(0);
    }
}
