package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceType {
    BUILTIN("builtin"),
    MCP("mcp"),
    KNOWLEDGE("knowledge");

    @EnumValue
    @JsonValue
    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
