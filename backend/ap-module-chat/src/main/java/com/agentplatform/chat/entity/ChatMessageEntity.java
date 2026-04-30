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
@TableName(value = "chat_messages", autoResultMap = true)
public class ChatMessageEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID sessionId;

    private String role;

    private String content;

    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String toolCalls;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String toolResults;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String attachments;

    private UUID agentId;

    private String modelId;

    private Integer stepCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String usage;

    private OffsetDateTime createdAt;
}
