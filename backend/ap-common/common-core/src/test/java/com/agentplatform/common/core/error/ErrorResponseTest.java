package com.agentplatform.common.core.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesInDesignedShape() throws Exception {
        ErrorResponse resp = ErrorResponse.of(
                "FILE_SIZE_EXCEEDED",
                "文件大小超过限制",
                Map.of("max_size_bytes", 52428800L, "actual_size_bytes", 78643200L),
                "req_abc123");

        String json = mapper.writeValueAsString(resp);
        JsonNode root = mapper.readTree(json);

        assertThat(root.has("error")).isTrue();
        JsonNode error = root.get("error");
        assertThat(error.get("code").asText()).isEqualTo("FILE_SIZE_EXCEEDED");
        assertThat(error.get("message").asText()).isEqualTo("文件大小超过限制");
        assertThat(error.get("details").get("max_size_bytes").asLong()).isEqualTo(52428800L);
        assertThat(error.get("requestId").asText()).isEqualTo("req_abc123");
    }
}
