package com.agentplatform.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("custom_models")
public class CustomModelEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID ownerId;

    private String name;

    private String apiUrl;

    private byte[] apiKeyEnc;

    private String connectionStatus;

    private String lastError;

    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
