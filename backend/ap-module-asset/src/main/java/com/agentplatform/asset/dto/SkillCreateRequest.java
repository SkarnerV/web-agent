package com.agentplatform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkillCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    private String triggerConditions;

    @NotBlank
    @Pattern(regexp = "yaml|markdown")
    private String format;

    @NotBlank
    private String content;
}
