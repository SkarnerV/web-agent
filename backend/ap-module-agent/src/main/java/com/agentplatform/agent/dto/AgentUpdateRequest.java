package com.agentplatform.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AgentUpdateRequest {

    @Size(max = 30)
    private String name;

    @Size(max = 200)
    private String description;

    private String avatar;

    private String systemPrompt;

    @Min(1)
    @Max(50)
    private Integer maxSteps;

    private String modelId;

    private List<ToolBindingRequest> toolBindings;

    private List<UUID> skillIds;

    private List<UUID> knowledgeBaseIds;

    private List<UUID> collaboratorAgentIds;

    @Min(0)
    private Long version;
}
