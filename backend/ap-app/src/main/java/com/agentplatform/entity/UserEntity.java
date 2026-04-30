package com.agentplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("users")
public class UserEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String w3Id;

    private String name;

    private String email;

    private String avatarUrl;

    private String role;

    private Boolean isActive;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
