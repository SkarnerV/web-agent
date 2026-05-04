package com.agentplatform.asset.controller;

import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.service.SkillService;
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
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillDetailVO> create(@Valid @RequestBody SkillCreateRequest request,
                                              @CurrentUser UserPrincipal user) {
        SkillDetailVO vo = skillService.create(request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping
    public ApiResponse<PageResult<SkillSummaryVO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "updated_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder,
            @CurrentUser UserPrincipal user) {
        PageResult<SkillSummaryVO> result = skillService.list(user.id(), search, page, pageSize, sortBy, sortOrder);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillDetailVO> getDetail(@PathVariable UUID id,
                                                 @CurrentUser UserPrincipal user) {
        SkillDetailVO vo = skillService.getDetail(id, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @PutMapping("/{id}")
    public ApiResponse<SkillDetailVO> update(@PathVariable UUID id,
                                              @Valid @RequestBody SkillUpdateRequest request,
                                              @CurrentUser UserPrincipal user) {
        SkillDetailVO vo = skillService.update(id, request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id,
                       @RequestParam(defaultValue = "false") boolean force,
                       @CurrentUser UserPrincipal user) {
        skillService.delete(id, user.id(), force);
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> export(@PathVariable UUID id,
                                                    @CurrentUser UserPrincipal user) {
        Map<String, Object> data = skillService.export(id, user.id());
        return ApiResponse.ok(data, RequestIdContext.current());
    }
}
