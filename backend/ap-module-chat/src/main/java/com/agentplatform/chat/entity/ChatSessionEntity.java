package com.agentplatform.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("chat_sessions")
public class ChatSessionEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private UUID userId;

    private UUID currentAgentId;

    private String title;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
