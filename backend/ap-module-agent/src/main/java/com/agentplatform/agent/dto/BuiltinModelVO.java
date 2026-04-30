package com.agentplatform.agent.dto;

import lombok.Data;

@Data
public class BuiltinModelVO {

    private String id;

    private String name;

    private String provider;

    private String description;

    private Boolean isDefault;

    private Boolean enabled;

    private Integer sortOrder;
}
