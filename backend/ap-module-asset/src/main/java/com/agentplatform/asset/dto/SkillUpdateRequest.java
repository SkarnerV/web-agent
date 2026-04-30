package com.agentplatform.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkillUpdateRequest {

    @Size(max = 100)
    private String name;

    private String description;

    private String triggerConditions;

    @Pattern(regexp = "yaml|markdown")
    private String format;

    private String content;

    @Min(0)
    private Long version;
}
