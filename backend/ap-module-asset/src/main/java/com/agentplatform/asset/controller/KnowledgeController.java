package com.agentplatform.asset.controller;

import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.service.KnowledgeBaseService;
import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // ───────── KB CRUD ─────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeBaseDetailVO> create(@Valid @RequestBody KnowledgeBaseCreateRequest request,
                                                      @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.create(request, user.id()), RequestIdContext.current());
    }

    @GetMapping
    public ApiResponse<PageResult<KnowledgeBaseSummaryVO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "updated_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.list(user.id(), search, page, pageSize, sortBy, sortOrder),
                RequestIdContext.current());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDetailVO> getDetail(@PathVariable UUID id, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.getDetail(id, user.id()), RequestIdContext.current());
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDetailVO> update(@PathVariable UUID id,
                                                      @Valid @RequestBody KnowledgeBaseUpdateRequest request,
                                                      @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.update(id, request, user.id()), RequestIdContext.current());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                     @RequestParam(defaultValue = "false") boolean force,
                                     @CurrentUser UserPrincipal user) {
        knowledgeBaseService.delete(id, user.id(), force);
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> export(@PathVariable UUID id, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.export(id, user.id()), RequestIdContext.current());
    }

    // ───────── Document Upload / List / Delete / Reindex (8.2, 8.5) ─────────

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Map<String, Object>> uploadDocument(@PathVariable UUID id,
                                                            @RequestParam("file") MultipartFile file,
                                                            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.uploadDocument(id, file, user.id()), RequestIdContext.current());
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<PageResult<KbDocumentVO>> listDocuments(@PathVariable UUID id,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                                @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.listDocuments(id, user.id(), page, pageSize),
                RequestIdContext.current());
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id,
                                             @PathVariable UUID docId,
                                             @CurrentUser UserPrincipal user) {
        knowledgeBaseService.deleteDocument(id, docId, user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @PostMapping("/{id}/documents/{docId}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> reindexDocument(@PathVariable UUID id,
                                              @PathVariable UUID docId,
                                              @CurrentUser UserPrincipal user) {
        knowledgeBaseService.reindexDocument(id, docId, user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    // ───────── Semantic Search (8.4) ─────────

    @PostMapping("/{id}/search")
    public ApiResponse<List<KbSearchResult>> search(@PathVariable UUID id,
                                                     @Valid @RequestBody KbSearchRequest request,
                                                     @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(knowledgeBaseService.search(id, request.getQuery(), request.getTopK(), user.id()),
                RequestIdContext.current());
    }
}
