package com.agentplatform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    private String indexConfig;
}
