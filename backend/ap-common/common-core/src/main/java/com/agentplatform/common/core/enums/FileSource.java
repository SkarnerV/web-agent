package com.agentplatform.common.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FileSource {
    CHAT_UPLOAD("chat_upload"),
    CHAT_GENERATED("chat_generated"),
    KNOWLEDGE("knowledge"),
    ASSET("asset");

    @EnumValue
    @JsonValue
    private final String value;

    FileSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
