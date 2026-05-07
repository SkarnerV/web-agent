package com.agentplatform.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuestionAnswerRequest {

    @NotBlank
    private String questionId;

    private List<String> selectedOptionIds;

    private String answerText;
}
