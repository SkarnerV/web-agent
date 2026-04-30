package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum McpProtocol {
    SSE("sse"),
    STREAMABLE_HTTP("streamable_http");

    @EnumValue
    @JsonValue
    private final String value;

    McpProtocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
