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
@TableName(value = "asset_versions", autoResultMap = true)
public class AssetVersionEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String assetType;

    private UUID assetId;

    private String version;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String configSnapshot;

    private String releaseNotes;

    private UUID publishedBy;

    private OffsetDateTime publishedAt;
}
