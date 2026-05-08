package com.agentplatform.chat.tool;

import com.agentplatform.common.core.error.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BuiltinToolExecutor")
class BuiltinToolExecutorTest {

    private BuiltinToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BuiltinToolExecutor(new ObjectMapper());
    }

    @Test
    @DisplayName("question requires options and always allows open-ended free text")
    void questionRequiresOptionsAndAlwaysAllowsOpenEndedFreeText() {
        BuiltinToolExecutor.PendingQuestion question = executor.parseQuestion(
                "call_question_1",
                """
                        {
                          "question": "What is your favorite color?",
                          "options": [
                            {"id": "red", "label": "Red"},
                            {"id": "blue", "label": "Blue"},
                            {"id": "green", "label": "Green"}
                          ],
                          "allow_free_text": false
                        }
                        """);

        assertThat(question.toolCallId()).isEqualTo("call_question_1");
        assertThat(question.question()).isEqualTo("What is your favorite color?");
        assertThat(question.allowFreeText()).isTrue();
        assertThat(question.options()).hasSize(3);
    }

    @Test
    @DisplayName("question rejects fewer than three options")
    void questionRejectsFewerThanThreeOptions() {
        assertThatThrownBy(() -> executor.parseQuestion(
                "call_question_1",
                """
                        {
                          "question": "Pick one",
                          "allow_free_text": true,
                          "options": [
                            {"id": "a", "label": "A"},
                            {"id": "b", "label": "B"}
                          ]
                        }
                        """))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getDetails())
                        .containsEntry("reason",
                                "question.options must contain 3-6 options"));
    }
}
