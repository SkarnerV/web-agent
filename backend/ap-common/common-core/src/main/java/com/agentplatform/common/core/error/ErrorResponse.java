package com.agentplatform.common.core.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 统一错误响应格式（设计文档 §6.2）。
 *
 * <pre>
 * {
 *   "error": {
 *     "code": "FILE_SIZE_EXCEEDED",
 *     "message": "...",
 *     "details": { ... },
 *     "request_id": "req_abc123"
 *   }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message, Map<String, Object> details, String requestId) {
        return new ErrorResponse(new ErrorBody(code, message, details, requestId));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            Map<String, Object> details,
            String requestId
    ) {}
}
