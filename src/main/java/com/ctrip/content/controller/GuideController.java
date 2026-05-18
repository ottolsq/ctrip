package com.ctrip.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.service.GuideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 攻略控制器。
 *
 * <p>公开接口：列表查询、详情查看
 * 需 JWT 认证：发布、编辑、删除、点赞
 */
@RestController
@RequestMapping("/api/v1/guides")
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    /**
     * 分页查询攻略列表（公开，仅已发布）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<GuideListResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "create_time") String sortBy) {
        Page<GuideListResponse> result = guideService.listGuides(page, limit, destinationId, authorId, keyword, sortBy);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 获取攻略详情（公开，浏览量 +1）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuideResponse>> getDetail(@PathVariable Long id) {
        GuideResponse result = guideService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 发布攻略（需 JWT 认证）。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GuideResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateGuideRequest request) {
        GuideResponse result = guideService.createGuide(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 编辑攻略（需 JWT 认证，仅作者）。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuideResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateGuideRequest request) {
        GuideResponse result = guideService.updateGuide(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 删除攻略（需 JWT 认证，仅作者）。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        guideService.deleteGuide(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 点赞攻略（需 JWT 认证）。
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> like(@PathVariable Long id) {
        guideService.likeGuide(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 取消点赞（需 JWT 认证）。
     */
    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlike(@PathVariable Long id) {
        guideService.unlikeGuide(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
