package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IndexStatus {
    PENDING_SCAN("pending_scan"),
    PENDING("pending"),
    INDEXING("indexing"),
    INDEXED("indexed"),
    FAILED("failed");

    @EnumValue
    @JsonValue
    private final String value;

    IndexStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
