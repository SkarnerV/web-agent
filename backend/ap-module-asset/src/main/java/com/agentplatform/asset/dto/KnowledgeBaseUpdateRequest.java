package com.agentplatform.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    @Size(max = 100)
    private String name;

    private String description;

    private String indexConfig;

    @Min(0)
    private Long version;
}
