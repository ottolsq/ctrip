package com.ctrip.operation.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.operation.dto.PublicRecommendationResponse;
import com.ctrip.operation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开推荐位接口：按 slot 返回启用且窗口内的推荐，并装配目标信息。
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class PublicRecommendationController {

    private final RecommendationService recommendationService;

    public PublicRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicRecommendationResponse>>> list(
            @RequestParam(defaultValue = "home_featured") String slot) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.listActive(slot, LocalDateTime.now())));
    }
}
