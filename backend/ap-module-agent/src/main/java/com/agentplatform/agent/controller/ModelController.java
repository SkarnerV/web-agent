package com.agentplatform.agent.controller;

import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.agent.dto.CustomModelCreateRequest;
import com.agentplatform.agent.dto.CustomModelUpdateRequest;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.agent.service.ModelService;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/builtin")
    public ApiResponse<List<BuiltinModelVO>> listBuiltin(@CurrentUser UserPrincipal user) {
        List<BuiltinModelVO> result = modelService.listBuiltinModels();
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/all")
    public ApiResponse<List<ModelInfo>> listAll(@CurrentUser UserPrincipal user) {
        List<ModelInfo> result = modelService.listAllModels(user.id());
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping
    public ApiResponse<List<CustomModelVO>> listCustom(@CurrentUser UserPrincipal user) {
        List<CustomModelVO> result = modelService.listCustomModels(user.id());
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomModelVO> create(@Valid @RequestBody CustomModelCreateRequest request,
                                              @CurrentUser UserPrincipal user) {
        CustomModelVO vo = modelService.createCustomModel(request, user.id());
        return ApiResponse.created(vo, RequestIdContext.current());
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomModelVO> update(@PathVariable UUID id,
                                              @Valid @RequestBody CustomModelUpdateRequest request,
                                              @CurrentUser UserPrincipal user) {
        CustomModelVO vo = modelService.updateCustomModel(id, request, user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                     @CurrentUser UserPrincipal user) {
        modelService.deleteCustomModel(id, user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @GetMapping("/{id}/agents")
    public ApiResponse<Map<String, Object>> listAffectedAgents(@PathVariable UUID id) {
        List<String> agentNames = modelService.listAgentNamesByModelId(id.toString());
        Map<String, Object> result = Map.of(
                "count", agentNames.size(),
                "agents", agentNames);
        return ApiResponse.ok(result, RequestIdContext.current());
    }
}
