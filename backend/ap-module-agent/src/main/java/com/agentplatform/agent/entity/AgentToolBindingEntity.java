package com.agentplatform.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "agent_tool_bindings", autoResultMap = true)
public class AgentToolBindingEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID agentId;

    private String sourceType;

    private UUID sourceId;

    private String toolName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String toolSchemaSnapshot;

    private Boolean enabled;

    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
