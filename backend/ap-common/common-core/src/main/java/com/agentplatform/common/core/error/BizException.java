package com.agentplatform.common.core.error;

import java.util.Collections;
import java.util.Map;

/**
 * 业务异常。所有可预期的业务错误均通过此异常向调用方传播。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), Collections.emptyMap(), null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyMap(), null);
    }

    public BizException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, errorCode.getDefaultMessage(), details, null);
    }

    public BizException(ErrorCode errorCode, String message, Map<String, Object> details) {
        this(errorCode, message, details, null);
    }

    public BizException(ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
