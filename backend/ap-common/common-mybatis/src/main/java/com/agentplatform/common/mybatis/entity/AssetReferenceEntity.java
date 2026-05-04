package com.agentplatform.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "asset_references", autoResultMap = true)
public class AssetReferenceEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String referrerType;

    private UUID referrerId;

    private String refereeType;

    private UUID refereeId;

    private String refKind;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String configParams;
}
