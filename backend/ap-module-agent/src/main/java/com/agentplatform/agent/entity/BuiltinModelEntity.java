package com.agentplatform.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName(value = "builtin_models", autoResultMap = true)
public class BuiltinModelEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String provider;

    private String description;

    private Boolean isDefault;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String config;

    private Boolean enabled;

    private Integer sortOrder;
}
