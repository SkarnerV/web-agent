package com.agentplatform.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ContinueRequest {

    @NotNull
    private UUID sessionStateId;
}
