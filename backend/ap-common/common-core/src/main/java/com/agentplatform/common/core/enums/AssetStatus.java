package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    @EnumValue
    @JsonValue
    private final String value;

    AssetStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
