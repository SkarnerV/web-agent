package com.agentplatform.agent.controller;

import com.agentplatform.agent.dto.*;
import com.agentplatform.agent.service.AgentService;
import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.error.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    // ───────── 3.1 CRUD ─────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentDetailVO> create(@Valid @RequestBody AgentCreateRequest request,
                                             @CurrentUser UserPrincipal user) {
        AgentDetailVO vo = agentService.create(request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping
    public ApiResponse<PageResult<AgentSummaryVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "updated_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder,
            @CurrentUser UserPrincipal user) {
        PageResult<AgentSummaryVO> result = agentService.list(user.id(), status, search, page, pageSize, sortBy, sortOrder);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentDetailVO> getDetail(@PathVariable UUID id,
                                                @CurrentUser UserPrincipal user) {
        AgentDetailVO vo = agentService.getDetail(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentDetailVO> update(@PathVariable UUID id,
                                             @Valid @RequestBody AgentUpdateRequest request,
                                             @CurrentUser UserPrincipal user) {
        AgentDetailVO vo = agentService.update(id, request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestParam(defaultValue = "false") boolean force,
                       @CurrentUser UserPrincipal user) {
        agentService.delete(id, user.id(), force);
    }

    // ───────── 3.2 Duplicate ─────────

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentDetailVO> duplicate(@PathVariable UUID id,
                                                @CurrentUser UserPrincipal user) {
        AgentDetailVO vo = agentService.duplicate(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    // ───────── 3.3 Export / Import ─────────

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> export(@PathVariable UUID id,
                                                   @CurrentUser UserPrincipal user) {
        Map<String, Object> data = agentService.export(id, user.id());
        return ApiResponse.ok(data, RequestIdContext.current());
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentImportResult> importAgent(@RequestParam("file") MultipartFile file,
                                                      @CurrentUser UserPrincipal user) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        AgentImportResult result = agentService.importAgent(content, user.id());
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleAgentValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        details.put("fields", fieldErrors);
        ErrorCode code = ErrorCode.AGENT_VALIDATION_FAILED;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code.name(), code.getDefaultMessage(), details,
                        RequestIdContext.current()));
    }

    // ───────── 3.4 Version Management ─────────

    @GetMapping("/{id}/versions")
    public ApiResponse<List<AssetVersionVO>> listVersions(@PathVariable UUID id,
                                                          @CurrentUser UserPrincipal user) {
        List<AssetVersionVO> versions = agentService.listVersions(id, user.id());
        return ApiResponse.ok(versions, RequestIdContext.current());
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    public ApiResponse<AgentDetailVO> rollback(@PathVariable UUID id,
                                               @PathVariable UUID versionId,
                                               @CurrentUser UserPrincipal user) {
        AgentDetailVO vo = agentService.rollback(id, versionId, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }
}
