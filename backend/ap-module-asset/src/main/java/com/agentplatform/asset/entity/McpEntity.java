package com.agentplatform.asset.entity;

import com.agentplatform.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.agentplatform.common.mybatis.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "mcps", autoResultMap = true)
public class McpEntity extends BaseEntity {

    private UUID ownerId;

    private String name;

    private String description;

    private String url;

    private String protocol;

    private byte[] authHeadersEnc;

    private String jsonConfig;

    private Boolean enabled;

    private String connectionStatus;

    private String lastError;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String toolsDiscovered;

    private String status;

    private String visibility;

    private String currentVersion;

    private Boolean hasUnpublishedChanges;
}
