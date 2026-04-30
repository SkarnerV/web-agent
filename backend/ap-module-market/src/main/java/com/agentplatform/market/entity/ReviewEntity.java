package com.agentplatform.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("reviews")
public class ReviewEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID marketItemId;

    private UUID userId;

    private Short rating;

    private String comment;

    private OffsetDateTime createdAt;
}
