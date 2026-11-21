package com.ctrip.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.AdminRejectGuideRequest;
import com.ctrip.content.dto.response.AdminGuideListItemResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.service.AdminGuideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端攻略审核控制器。ADMIN / CONTENT_OPERATOR 可访问。
 */
@RestController
@RequestMapping("/api/v1/admin/guides")
@PreAuthorize("hasAnyRole('ADMIN','CONTENT_OPERATOR')")
public class AdminGuideController {

    private final AdminGuideService adminGuideService;

    public AdminGuideController(AdminGuideService adminGuideService) {
        this.adminGuideService = adminGuideService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminGuideListItemResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) GuideStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminGuideService.listGuides(page, size, status, keyword, authorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuideResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminGuideService.getDetail(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId) {
        adminGuideService.approve(id, operatorId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId,
            @Valid @RequestBody AdminRejectGuideRequest request) {
        adminGuideService.reject(id, operatorId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
