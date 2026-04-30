package com.agentplatform.agent.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AgentDetailVO {

    private UUID id;

    private String name;

    private String description;

    private String avatar;

    private String systemPrompt;

    private Integer maxSteps;

    private String modelId;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;

    private UUID ownerId;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long version;

    private List<ToolBindingVO> toolBindings;

    private List<UUID> skillIds;

    private List<UUID> knowledgeBaseIds;
}
