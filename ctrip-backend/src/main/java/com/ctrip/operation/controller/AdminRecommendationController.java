package com.ctrip.operation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.operation.dto.RecommendationRequest;
import com.ctrip.operation.dto.RecommendationResponse;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端推荐位控制器。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/v1/admin/recommendations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    public AdminRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecommendationResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String slot,
            @RequestParam(required = false) DisplayStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.listRecommendations(page, size, slot, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> create(
            @Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.ok(recommendationService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        recommendationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
