package com.agentplatform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class McpCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    private String url;

    @NotBlank
    private String protocol;

    private String authHeaders;

    private String jsonConfig;
}
