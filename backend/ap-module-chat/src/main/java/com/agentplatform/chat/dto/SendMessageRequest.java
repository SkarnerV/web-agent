package com.agentplatform.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotBlank
    private String content;

    private List<UUID> attachments;

    private String idempotencyKey;
}
