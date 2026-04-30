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
@TableName("organizations")
public class OrganizationEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String w3OrgId;

    private String name;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
