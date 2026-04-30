package com.agentplatform.market.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ReviewVO {

    private UUID id;

    private UUID marketItemId;

    private UUID userId;

    private String userName;

    private Short rating;

    private String comment;

    private OffsetDateTime createdAt;
}
