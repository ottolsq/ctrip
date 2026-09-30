package com.ctrip.content.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateAttractionRequest;
import com.ctrip.content.dto.request.UpdateAttractionRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.service.AttractionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端景点控制器（需 ADMIN 或 CONTENT_OPERATOR 角色）。
 *
 * <p>路径：/api/v1/admin/attractions/**，由 SecurityConfig 保护。
 */
@RestController
@RequestMapping("/api/v1/admin/attractions")
public class AdminAttractionController {

    private final AttractionService attractionService;

    public AdminAttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    /**
     * 创建景点。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AttractionResponse>> create(@Valid @RequestBody CreateAttractionRequest request) {
        AttractionResponse result = attractionService.createAttraction(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 更新景点。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttractionRequest request) {
        attractionService.updateAttraction(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 删除景点。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        attractionService.deleteAttraction(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
