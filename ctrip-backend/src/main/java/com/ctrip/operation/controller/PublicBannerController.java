package com.ctrip.operation.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.operation.dto.BannerResponse;
import com.ctrip.operation.service.BannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开轮播图接口：仅返回启用且处于展示时间窗内的记录。
 */
@RestController
@RequestMapping("/api/v1/banners")
public class PublicBannerController {

    private final BannerService bannerService;

    public PublicBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(
                bannerService.listActiveBanners(LocalDateTime.now())));
    }
}
