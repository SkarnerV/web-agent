package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ScanStatus {
    PENDING("pending"),
    CLEAN("clean"),
    INFECTED("infected"),
    ERROR("error");

    @EnumValue
    @JsonValue
    private final String value;

    ScanStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
