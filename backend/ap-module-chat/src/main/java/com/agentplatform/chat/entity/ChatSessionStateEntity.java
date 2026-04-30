package com.agentplatform.chat.entity;

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
@TableName(value = "chat_session_states", autoResultMap = true)
public class ChatSessionStateEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID sessionId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String contextSnapshot;

    private Integer stepCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String toolCache;

    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;
}
