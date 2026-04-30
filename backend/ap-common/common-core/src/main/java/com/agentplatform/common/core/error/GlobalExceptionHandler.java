package com.agentplatform.common.core.error;

import com.agentplatform.common.core.trace.RequestIdContext;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一异常处理（设计文档 §6.2）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBiz(BizException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("[BizException] code={}, msg={}", code.name(), ex.getMessage(), ex);
        } else {
            log.warn("[BizException] code={}, msg={}, details={}", code.name(), ex.getMessage(), ex.getDetails());
        }
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(code.name(), ex.getMessage(), ex.getDetails(), RequestIdContext.current()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        details.put("fields", fieldErrors);
        return build(ErrorCode.INVALID_REQUEST, "请求参数校验失败", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("violations", ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new)));
        return build(ErrorCode.INVALID_REQUEST, "请求参数校验失败", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.INVALID_REQUEST, "请求体格式错误", Map.of("hint", "JSON 解析失败"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(ErrorCode.INVALID_REQUEST, "缺少请求参数: " + ex.getParameterName(),
                Map.of("parameter", ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.INVALID_REQUEST, "请求参数类型错误: " + ex.getName(),
                Map.of("parameter", ex.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED", ex.getMessage(), Map.of(),
                        RequestIdContext.current()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of("NOT_FOUND", "资源不存在", Map.of("path", ex.getRequestURL()),
                        RequestIdContext.current()));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(406)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of("NOT_ACCEPTABLE", "不支持请求的 Accept 类型", Map.of(),
                        RequestIdContext.current()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return build(ErrorCode.FILE_SIZE_EXCEEDED, ErrorCode.FILE_SIZE_EXCEEDED.getDefaultMessage(),
                Map.of("max_size_bytes", ex.getMaxUploadSize()));
    }

    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(org.springframework.web.multipart.MultipartException ex) {
        return build(ErrorCode.INVALID_REQUEST, "请求必须为 multipart/form-data 格式",
                Map.of("hint", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        log.error("[Unhandled] {}", ex.getMessage(), ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR.name(), "服务内部错误", Map.of(),
                        RequestIdContext.current()));
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, Map<String, Object> details) {
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(code.name(), message, details, RequestIdContext.current()));
    }
}
