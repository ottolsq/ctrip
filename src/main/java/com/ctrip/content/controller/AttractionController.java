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
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) String keyword) {
        Page<AttractionResponse> result = attractionService.listAttractions(page, limit, destinationId, keyword);
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

    // ==================== 管理端接口 ====================

    /**
     * 管理端：创建景点。
     */
    @PostMapping("/api/v1/admin/attractions")
    public ResponseEntity<ApiResponse<AttractionResponse>> create(@Valid @RequestBody CreateAttractionRequest request) {
        AttractionResponse result = attractionService.createAttraction(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 管理端：更新景点。
     */
    @PutMapping("/api/v1/admin/attractions/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttractionRequest request) {
        attractionService.updateAttraction(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 管理端：删除景点。
     */
    @DeleteMapping("/api/v1/admin/attractions/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        attractionService.deleteAttraction(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
