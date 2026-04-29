package com.agentplatform.web;

import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 任务 1.3 冒烟端点：
 * <ul>
 *   <li>{@code GET /api/v1/health} 验证 Virtual Threads 已启用</li>
 *   <li>{@code GET /api/v1/health/sse} 验证 SseEmitter 推送 3 个事件后正常关闭</li>
 *   <li>{@code GET /api/v1/health/me} 验证 {@link CurrentUser} 注入</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/health")
public class SmokeController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("thread", Thread.currentThread().toString());
        data.put("virtual", Thread.currentThread().isVirtual());
        return ApiResponse.ok(data, RequestIdContext.current());
    }

    @GetMapping("/me")
    public ApiResponse<UserPrincipal> me(@CurrentUser UserPrincipal user) {
        return ApiResponse.ok(user, RequestIdContext.current());
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        SseEmitter emitter = new SseEmitter(30_000L);
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    emitter.send(SseEmitter.event()
                            .id("evt_" + String.format("%05d", i))
                            .name("ping")
                            .data(Map.of("seq", i, "thread", Thread.currentThread().toString())));
                    Thread.sleep(50);
                }
                emitter.complete();
            } catch (IOException | InterruptedException ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
