package com.agentplatform.market.controller;

import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import com.agentplatform.market.dto.*;
import com.agentplatform.market.service.MarketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MarketItemVO> publish(@Valid @RequestBody PublishRequest request,
                                              @CurrentUser UserPrincipal user) {
        MarketItemVO vo = marketService.publish(request, user.id());
        return ApiResponse.created(vo, RequestIdContext.current());
    }

    @PutMapping("/items/{id}/visibility")
    public ApiResponse<MarketItemVO> updateVisibility(@PathVariable UUID id,
                                                       @Valid @RequestBody VisibilityUpdateRequest body,
                                                       @CurrentUser UserPrincipal user) {
        MarketItemVO vo = marketService.updateVisibility(id, body.getVisibility(), user.id());
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping("/items")
    public ApiResponse<PageResult<MarketItemVO>> listItems(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "updated_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder) {
        PageResult<MarketItemVO> result = marketService.listItems(
                type, category, search, tags, page, pageSize, sortBy, sortOrder);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @GetMapping("/items/{id}")
    public ApiResponse<MarketItemDetailVO> getItemDetail(@PathVariable UUID id) {
        MarketItemDetailVO vo = marketService.getItemDetail(id);
        return ApiResponse.ok(vo, RequestIdContext.current());
    }

    @GetMapping("/featured")
    public ApiResponse<PageResult<MarketItemVO>> getFeatured(
            @RequestParam(required = false) String type) {
        PageResult<MarketItemVO> result = marketService.getFeaturedItems(type);
        return ApiResponse.ok(result, RequestIdContext.current());
    }

    @PostMapping("/items/{id}/favorite")
    public ApiResponse<Void> addFavorite(@PathVariable UUID id,
                                          @CurrentUser UserPrincipal user) {
        marketService.addFavorite(id, user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @DeleteMapping("/items/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeFavorite(@PathVariable UUID id,
                                             @CurrentUser UserPrincipal user) {
        marketService.removeFavorite(id, user.id());
        return ApiResponse.ok(null, RequestIdContext.current());
    }

    @PostMapping("/items/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewVO> createReview(@PathVariable UUID id,
                                              @Valid @RequestBody ReviewCreateRequest request,
                                              @CurrentUser UserPrincipal user) {
        ReviewVO vo = marketService.createReview(id, request, user.id());
        return ApiResponse.created(vo, RequestIdContext.current());
    }

    @GetMapping("/items/{id}/reviews")
    public ApiResponse<List<ReviewVO>> listReviews(@PathVariable UUID id,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        List<ReviewVO> reviews = marketService.listReviews(id, page, pageSize);
        return ApiResponse.ok(reviews, RequestIdContext.current());
    }

    @PostMapping("/items/{id}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> importItem(@PathVariable UUID id,
                                                        @CurrentUser UserPrincipal user) {
        Map<String, Object> result = marketService.importItem(id, user.id());
        return ApiResponse.created(result, RequestIdContext.current());
    }
}
