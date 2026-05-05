package com.agentplatform.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.agentplatform.common.mybatis.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "market_items", autoResultMap = true)
public class MarketItemEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String assetType;

    private UUID assetId;

    private UUID currentVersionId;

    private UUID authorId;

    private String status;

    private String visibility;

    private String category;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String tags;

    private Long useCount;

    private Long favoriteCount;

    private BigDecimal avgRating;

    private Integer reviewCount;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
