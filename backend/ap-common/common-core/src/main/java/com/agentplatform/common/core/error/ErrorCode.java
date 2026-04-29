package com.agentplatform.common.core.error;

import org.springframework.http.HttpStatus;

/**
 * 全局错误码（设计文档 §6.1）。
 *
 * <p>SSE 流内事件错误（CHAT_STEP_LIMIT / CHAT_MODEL_ERROR / CHAT_TOOL_ERROR / MCP_TOOL_CALL_FAILED）
 * 在 HTTP 维度不暴露状态码，仅通过 SSE error 事件下发；这里为统一管理仍保留枚举常量，
 * 但 HTTP 状态默认置为 200，调用方应避免通过 GlobalExceptionHandler 抛出这类码。</p>
 */
public enum ErrorCode {

    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token 已过期"),
    AUTH_REFRESH_FAILED(HttpStatus.UNAUTHORIZED, "Refresh Token 无效，请重新登录"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "无权访问该资源"),
    AUTH_OAUTH_FAILED(HttpStatus.BAD_GATEWAY, "OAuth 回调失败"),

    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "资产不存在或已被删除"),
    ASSET_NAME_DUPLICATE(HttpStatus.CONFLICT, "同用户下资产名称重复"),
    ASSET_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "资产权限不足"),
    ASSET_DELETE_CONFLICT(HttpStatus.CONFLICT, "资产被其他 Agent 引用，无法删除"),
    ASSET_VISIBILITY_INVALID(HttpStatus.BAD_REQUEST, "该资产类型不支持此可见性"),
    ASSET_VERSION_CONFLICT(HttpStatus.CONFLICT, "版本号已存在"),
    ASSET_OPTIMISTIC_LOCK(HttpStatus.CONFLICT, "并发编辑冲突，请刷新后重试"),

    AGENT_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Agent 配置校验失败"),
    AGENT_IMPORT_INVALID(HttpStatus.BAD_REQUEST, "导入文件格式不正确"),

    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "会话不存在"),
    CHAT_IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "幂等键被复用于不同请求体"),
    CHAT_STEP_LIMIT(HttpStatus.OK, "已达到最大步骤数"),
    CHAT_MODEL_ERROR(HttpStatus.OK, "模型推理失败"),
    CHAT_TOOL_ERROR(HttpStatus.OK, "工具调用失败"),

    MCP_CONNECTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "MCP 连接验证失败"),
    MCP_TOOL_CALL_FAILED(HttpStatus.OK, "远程工具调用失败"),
    MCP_PROTOCOL_INVALID(HttpStatus.BAD_REQUEST, "不支持的 MCP 协议"),

    KB_INDEX_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "知识库索引构建失败"),
    KB_DOC_TYPE_UNSUPPORTED(HttpStatus.BAD_REQUEST, "不支持的文档格式"),
    KB_DOC_SCAN_PENDING(HttpStatus.CONFLICT, "文档安全扫描尚未完成"),
    KB_DOC_SCAN_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "文档安全扫描未通过"),
    KB_DOC_REINDEX_BLOCKED(HttpStatus.CONFLICT, "文档扫描未通过，不允许重新索引"),

    FILE_TYPE_REJECTED(HttpStatus.BAD_REQUEST, "文件类型不在白名单"),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "文件大小超过限制"),
    FILE_SCAN_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "文件安全扫描未通过"),
    FILE_EXPIRED(HttpStatus.GONE, "文件已过期"),
    FILE_LINK_EXPIRED(HttpStatus.FORBIDDEN, "下载链接无效或已过期"),

    MODEL_CONNECTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "模型 API 连通性验证失败"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "请求参数不合法");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
