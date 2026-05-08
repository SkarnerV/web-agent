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
    @DisplayName("question supports open-ended free text without options")
    void questionSupportsOpenEndedFreeTextWithoutOptions() {
        BuiltinToolExecutor.PendingQuestion question = executor.parseQuestion(
                "call_question_1",
                """
                        {
                          "question": "What is your favorite color?",
                          "allow_free_text": true,
                          "options": []
                        }
                        """);

        assertThat(question.toolCallId()).isEqualTo("call_question_1");
        assertThat(question.question()).isEqualTo("What is your favorite color?");
        assertThat(question.allowFreeText()).isTrue();
        assertThat(question.options()).isEmpty();
    }

    @Test
    @DisplayName("question rejects empty options when free text is disabled")
    void questionRejectsEmptyOptionsWithoutFreeText() {
        assertThatThrownBy(() -> executor.parseQuestion(
                "call_question_1",
                """
                        {
                          "question": "Pick one",
                          "options": []
                        }
                        """))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getDetails())
                        .containsEntry("reason",
                                "question.options must contain 1-6 options unless allow_free_text is true"));
    }
}
