package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectionStatus {
    ONLINE("online"),
    OFFLINE("offline"),
    ERROR("error"),
    CONNECTED("connected"),
    FAILED("failed");

    @EnumValue
    @JsonValue
    private final String value;

    ConnectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
