package com.agentplatform.asset.entity;

import com.agentplatform.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "skills", autoResultMap = true)
public class SkillEntity extends BaseEntity {

    private UUID ownerId;

    private String name;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String triggerConditions;

    private String format;

    private String content;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;
}
