package com.agentplatform.common.core.trace;

import org.slf4j.MDC;

/**
 * Request-ID 上下文工具，配合 {@link RequestIdFilter} 使用。
 */
public final class RequestIdContext {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private RequestIdContext() {}

    public static String currentOrEmpty() {
        String id = MDC.get(MDC_KEY);
        return id == null ? "" : id;
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
