package com.agentplatform.agent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomModelUpdateRequest {

    @Size(max = 100)
    private String name;

    private String apiUrl;

    private String apiKey;
}
