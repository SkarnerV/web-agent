package com.agentplatform.common.core.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void httpStatusMappings() {
        assertThat(ErrorCode.FILE_SIZE_EXCEEDED.getHttpStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(ErrorCode.AUTH_FORBIDDEN.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ErrorCode.ASSET_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.ASSET_DELETE_CONFLICT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.MCP_CONNECTION_FAILED.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ErrorCode.FILE_EXPIRED.getHttpStatus()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void allCodesHaveDefaultMessage() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.getDefaultMessage())
                    .as("ErrorCode %s should have default message", code)
                    .isNotBlank();
        }
    }
}
