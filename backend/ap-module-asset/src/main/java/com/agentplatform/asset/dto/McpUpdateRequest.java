package com.agentplatform.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class McpUpdateRequest {

    @Size(max = 100)
    private String name;

    private String url;

    private String protocol;

    private String authHeaders;

    private String jsonConfig;

    @Min(0)
    private Long version;
}
