package com.agentplatform;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.error.GlobalExceptionHandler;
import com.agentplatform.common.core.trace.RequestIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorFormatContractTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RestController
    @RequestMapping("/test/errors")
    static class TestController {

        static class TestRequest {
            @NotBlank private String name;
            @Size(min = 5, max = 10) private String code;
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getCode() { return code; }
            public void setCode(String code) { this.code = code; }
        }

        @PostMapping("/validation")
        String triggerValidation(@Valid @RequestBody TestRequest req) {
            return "ok";
        }

        @GetMapping("/not-found")
        String triggerNotFound() {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }

        @GetMapping("/conflict")
        String triggerConflict() {
            throw new BizException(ErrorCode.ASSET_NAME_DUPLICATE);
        }

        @GetMapping("/unprocessable")
        String triggerUnprocessable() {
            throw new BizException(ErrorCode.MODEL_CONNECTION_FAILED,
                    Map.of("reason", "Connection refused"));
        }

        @GetMapping("/forbidden")
        String triggerForbidden() {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        @GetMapping("/bad-request")
        String triggerBadRequest() {
            throw new BizException(ErrorCode.INVALID_REQUEST,
                    Map.of("field", "type", "message", "unsupported asset type"));
        }

        @GetMapping("/file-too-large")
        String triggerFileTooLarge() {
            throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED,
                    Map.of("max_size_bytes", 10485760));
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new Filter() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response,
                                         FilterChain chain) throws IOException, ServletException {
                        MDC.put(RequestIdContext.MDC_KEY, "req_contract_test_000001");
                        try {
                            chain.doFilter(request, response);
                        } finally {
                            MDC.remove(RequestIdContext.MDC_KEY);
                        }
                    }
                }, "/*")
                .build();
    }

    @Nested
    @DisplayName("Error format contract")
    class ErrorFormat {

        @Test
        @DisplayName("404 Not Found — ASSET_NOT_FOUND")
        void notFound() throws Exception {
            mockMvc.perform(get("/test/errors/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("ASSET_NOT_FOUND"))
                    .andExpect(jsonPath("$.error.message").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("409 Conflict — ASSET_NAME_DUPLICATE")
        void conflict() throws Exception {
            mockMvc.perform(get("/test/errors/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("ASSET_NAME_DUPLICATE"))
                    .andExpect(jsonPath("$.error.message").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("422 Unprocessable — MODEL_CONNECTION_FAILED with details")
        void unprocessableEntity() throws Exception {
            mockMvc.perform(get("/test/errors/unprocessable"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("MODEL_CONNECTION_FAILED"))
                    .andExpect(jsonPath("$.error.details.reason").value("Connection refused"))
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("403 Forbidden — ASSET_PERMISSION_DENIED")
        void forbidden() throws Exception {
            mockMvc.perform(get("/test/errors/forbidden"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ASSET_PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.error.message").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("400 Bad Request — INVALID_REQUEST with details")
        void badRequest() throws Exception {
            mockMvc.perform(get("/test/errors/bad-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.error.details.field").value("type"))
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("413 Payload Too Large — FILE_SIZE_EXCEEDED")
        void payloadTooLarge() throws Exception {
            mockMvc.perform(get("/test/errors/file-too-large"))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.error.code").value("FILE_SIZE_EXCEEDED"))
                    .andExpect(jsonPath("$.error.details.max_size_bytes").value(10485760))
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("Validation failure returns 400 with field errors")
        void validationFailure() throws Exception {
            String body = objectMapper.writeValueAsString(
                    Map.of("name", "", "code", "ab"));

            mockMvc.perform(post("/test/errors/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.error.details.fields.name").isNotEmpty())
                    .andExpect(jsonPath("$.error.details.fields.code").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("Malformed JSON request body returns 400")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/test/errors/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.error.message").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }
    }
}
