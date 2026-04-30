package com.agentplatform.agent.entity;

import com.agentplatform.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("agents")
public class AgentEntity extends BaseEntity {

    private UUID ownerId;

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
}
