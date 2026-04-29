package com.agentplatform.common.core.response;

import java.util.List;

/**
 * 通用分页结果。
 */
public record PageResult<T>(
        List<T> data,
        long total,
        int page,
        int pageSize
) {
    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize);
    }
}
