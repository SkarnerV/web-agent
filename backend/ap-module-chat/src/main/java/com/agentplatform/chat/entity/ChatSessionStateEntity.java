package com.agentplatform.chat.entity;

import com.agentplatform.common.mybatis.handler.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "chat_session_states", autoResultMap = true)
public class ChatSessionStateEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private UUID sessionId;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String contextSnapshot;

    private Integer stepCount;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String toolCache;

    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;
}
