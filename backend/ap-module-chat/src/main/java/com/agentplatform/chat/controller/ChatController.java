package com.agentplatform.chat.controller;

import com.agentplatform.chat.dto.*;
import com.agentplatform.chat.orchestrator.ChatOrchestrator;
import com.agentplatform.chat.service.ChatSessionService;
import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatSessionService sessionService;
    private final ChatOrchestrator chatOrchestrator;

    public ChatController(ChatSessionService sessionService,
                          ChatOrchestrator chatOrchestrator) {
        this.sessionService = sessionService;
        this.chatOrchestrator = chatOrchestrator;
    }

    // ───────── 4.1 Session Management ─────────

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatSessionVO> createSession(@Valid @RequestBody CreateSessionRequest request,
                                                    @CurrentUser UserPrincipal user) {
        ChatSessionVO vo = sessionService.createSession(request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<ChatSessionVO>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @CurrentUser UserPrincipal user) {
        PageResult<ChatSessionVO> result = sessionService.listSessions(user.id(), page, pageSize);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDetailVO> getSession(@PathVariable UUID id,
                                                       @CurrentUser UserPrincipal user) {
        ChatSessionDetailVO detail = sessionService.getSessionDetail(id, user.id());
        return ApiResponse.ok(detail, RequestIdContext.current());
    }

    @DeleteMapping("/sessions/{id}/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMessages(@PathVariable UUID id,
                              @CurrentUser UserPrincipal user) {
        sessionService.clearMessages(id, user.id());
    }

    // ───────── 4.4 Send Message (SSE) ─────────

    @PostMapping(value = "/sessions/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable UUID id,
                                  @Valid @RequestBody SendMessageRequest request,
                                  @CurrentUser UserPrincipal user) {
        return chatOrchestrator.handleSendMessage(id, request, user.id());
    }

    // ───────── 4.7 Regenerate ─────────

    @PostMapping(value = "/sessions/{sessionId}/messages/{msgId}/regenerate",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerate(@PathVariable UUID sessionId,
                                 @PathVariable UUID msgId,
                                 @CurrentUser UserPrincipal user) {
        return chatOrchestrator.handleRegenerate(sessionId, msgId, user.id());
    }

    // ───────── 4.8 Switch Agent ─────────

    @PutMapping("/sessions/{id}/agent")
    public ApiResponse<ChatMessageVO> switchAgent(@PathVariable UUID id,
                                                  @RequestBody SwitchAgentRequest request,
                                                  @CurrentUser UserPrincipal user) {
        sessionService.switchAgent(id, request.getAgentId(), user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    // ───────── 4.6 Continue ─────────

    @PostMapping(value = "/sessions/{id}/continue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueExecution(@PathVariable UUID id,
                                        @Valid @RequestBody ContinueRequest request,
                                        @CurrentUser UserPrincipal user) {
        return chatOrchestrator.handleContinue(id, request.getSessionStateId(), user.id());
    }
}
