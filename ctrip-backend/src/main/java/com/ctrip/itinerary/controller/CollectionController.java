package com.ctrip.itinerary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.itinerary.dto.response.CollectionItemResponse;
import com.ctrip.itinerary.dto.response.CollectionStatusResponse;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;
import com.ctrip.itinerary.service.CollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用收藏控制器。
 *
 * <p>所有端点均需 JWT 认证。
 */
@RestController
@RequestMapping("/api/v1")
public class CollectionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * 我的收藏分页列表。
     */
    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<Page<CollectionItemResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CollectionTargetType type) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<CollectionItemResponse> result =
                collectionService.listCollections(userId, page, effectiveSize, type);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 查询当前用户是否已收藏目标。
     */
    @GetMapping("/collections/status")
    public ResponseEntity<ApiResponse<CollectionStatusResponse>> status(
            @AuthenticationPrincipal Long userId,
            @RequestParam CollectionTargetType targetType,
            @RequestParam Long targetId) {
        boolean collected = collectionService.isCollected(userId, targetType, targetId);
        return ResponseEntity.ok(ApiResponse.ok(new CollectionStatusResponse(collected)));
    }

    /**
     * 收藏攻略。
     */
    @PostMapping("/guides/{guideId}/collect")
    public ResponseEntity<ApiResponse<Void>> collectGuide(
            @PathVariable Long guideId,
            @AuthenticationPrincipal Long userId) {
        collectionService.collect(userId, CollectionTargetType.GUIDE, guideId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 取消收藏攻略。
     */
    @DeleteMapping("/guides/{guideId}/collect")
    public ResponseEntity<ApiResponse<Void>> uncollectGuide(
            @PathVariable Long guideId,
            @AuthenticationPrincipal Long userId) {
        collectionService.uncollect(userId, CollectionTargetType.GUIDE, guideId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 取消收藏任意目标（我的收藏页使用）。
     */
    @DeleteMapping("/collections/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<Void>> uncollect(
            @PathVariable CollectionTargetType targetType,
            @PathVariable Long targetId,
            @AuthenticationPrincipal Long userId) {
        collectionService.uncollect(userId, targetType, targetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
