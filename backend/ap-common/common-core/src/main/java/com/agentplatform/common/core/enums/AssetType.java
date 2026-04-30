package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetType {
    AGENT("agent"),
    SKILL("skill"),
    MCP("mcp");

    @EnumValue
    @JsonValue
    private final String value;

    AssetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
