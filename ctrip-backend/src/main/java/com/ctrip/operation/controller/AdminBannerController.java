package com.ctrip.operation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.operation.dto.BannerRequest;
import com.ctrip.operation.dto.BannerResponse;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.service.BannerService;
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
 * 管理端轮播图控制器。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BannerResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) DisplayStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(bannerService.listBanners(page, size, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> create(
            @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(bannerService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bannerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
