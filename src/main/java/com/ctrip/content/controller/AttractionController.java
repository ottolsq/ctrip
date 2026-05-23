package com.ctrip.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateAttractionRequest;
import com.ctrip.content.dto.request.UpdateAttractionRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.service.AttractionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 景点控制器。
 *
 * <p>公开接口：列表查询、详情查看（/api/v1/attractions/**）
 * 管理端接口：CRUD 操作（/api/v1/admin/attractions/**，需 ADMIN 角色）
 */
@RestController
@RequestMapping("/api/v1/attractions")
public class AttractionController {

    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    /**
     * 分页查询景点列表（公开）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AttractionResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) String keyword) {
        int effectiveLimit = Math.min(limit, 100);
        Page<AttractionResponse> result = attractionService.listAttractions(page, effectiveLimit, destinationId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 获取景点详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AttractionResponse>> getDetail(@PathVariable Long id) {
        AttractionResponse result = attractionService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
