package com.agentplatform.asset.controller;

import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.service.McpService;
import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mcps")
public class McpController {

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<McpDetailVO> create(@Valid @RequestBody McpCreateRequest request,
                                            @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.create(request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping
    public ApiResponse<PageResult<McpSummaryVO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "updated_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder,
            @CurrentUser UserPrincipal user) {
        PageResult<McpSummaryVO> result = mcpService.list(user.id(), search, page, pageSize, sortBy, sortOrder);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/{id}")
    public ApiResponse<McpDetailVO> getDetail(@PathVariable UUID id,
                                               @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.getDetail(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @PutMapping("/{id}")
    public ApiResponse<McpDetailVO> update(@PathVariable UUID id,
                                            @Valid @RequestBody McpUpdateRequest request,
                                            @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.update(id, request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                     @RequestParam(defaultValue = "false") boolean force,
                                     @CurrentUser UserPrincipal user) {
        mcpService.delete(id, user.id(), force);
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<McpDetailVO> toggle(@PathVariable UUID id,
                                            @RequestParam(defaultValue = "true") boolean enabled,
                                            @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.toggle(id, user.id(), enabled);
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> export(@PathVariable UUID id,
                                                    @CurrentUser UserPrincipal user) {
        Map<String, Object> data = mcpService.export(id, user.id());
        return ApiResponse.ok(data, RequestIdContext.current());
    }

    @PostMapping("/{id}/test")
    public ApiResponse<McpDetailVO> testConnection(@PathVariable UUID id,
                                                    @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.testConnection(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @PostMapping("/{id}/discover")
    public ApiResponse<McpDetailVO> discoverTools(@PathVariable UUID id,
                                                   @CurrentUser UserPrincipal user) {
        McpDetailVO vo = mcpService.discoverTools(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }
}
