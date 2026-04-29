package com.agentplatform.common.core.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 在每个请求中注入 {@code request_id}，可由客户端通过 {@code X-Request-Id} 头传入；
 * 同时写入 MDC 与响应头，便于全链路日志关联（设计文档 §6.2）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String existing = request.getHeader(RequestIdContext.HEADER);
        String requestId = (existing == null || existing.isBlank())
                ? "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)
                : existing;
        MDC.put(RequestIdContext.MDC_KEY, requestId);
        response.setHeader(RequestIdContext.HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }
}
