package com.ctrip.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.DestinationResponse;
import com.ctrip.content.service.DestinationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 目的地控制器。
 *
 * <p>公开接口：列表查询、详情查看（/api/v1/destinations/**）
 * 管理端接口：CRUD 操作（/api/v1/admin/destinations/**，需 ADMIN 角色）
 */
@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    /**
     * 分页查询目的地列表（公开）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DestinationResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String keyword) {
        Page<DestinationResponse> result = destinationService.listDestinations(page, limit, country, province, keyword);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 获取目的地详情（含关联景点列表）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DestinationResponse>> getDetail(@PathVariable Long id) {
        DestinationResponse result = destinationService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端：创建目的地。
     */
    @PostMapping("/api/v1/admin/destinations")
    public ResponseEntity<ApiResponse<DestinationResponse>> create(@Valid @RequestBody CreateDestinationRequest request) {
        DestinationResponse result = destinationService.createDestination(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 管理端：更新目的地。
     */
    @PutMapping("/api/v1/admin/destinations/{id}")
    public ResponseEntity<ApiResponse<DestinationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDestinationRequest request) {
        DestinationResponse result = destinationService.updateDestination(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 管理端：删除目的地。
     */
    @DeleteMapping("/api/v1/admin/destinations/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
