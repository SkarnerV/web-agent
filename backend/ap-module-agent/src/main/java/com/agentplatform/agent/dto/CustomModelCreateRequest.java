package com.agentplatform.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomModelCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    private String apiUrl;

    @NotBlank
    private String apiKey;
}
