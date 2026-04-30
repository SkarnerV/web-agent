package com.agentplatform.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MarketItemVO {

    private UUID id;

    private String assetType;

    private UUID assetId;

    private UUID currentVersionId;

    private UUID authorId;

    private String authorName;

    private String status;

    private String visibility;

    private String category;

    private String tags;

    private Long useCount;

    private Long favoriteCount;

    private BigDecimal avgRating;

    private Integer reviewCount;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
