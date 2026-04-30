package com.agentplatform.market.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("favorites")
public class FavoriteEntity {

    private UUID userId;

    private UUID marketItemId;

    private OffsetDateTime createdAt;
}
