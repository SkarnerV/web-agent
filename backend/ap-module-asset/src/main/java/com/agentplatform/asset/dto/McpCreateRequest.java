package com.agentplatform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class McpCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    @URL
    private String url;

    @NotBlank
    private String protocol;

    private String authHeaders;

    private String jsonConfig;
}
