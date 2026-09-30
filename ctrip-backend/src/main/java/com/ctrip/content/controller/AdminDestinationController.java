package com.ctrip.content.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.DestinationResponse;
import com.ctrip.content.service.DestinationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端目的地控制器（需 ADMIN 或 CONTENT_OPERATOR 角色）。
 *
 * <p>路径：/api/v1/admin/destinations/**，由 SecurityConfig 保护。
 */
@RestController
@RequestMapping("/api/v1/admin/destinations")
public class AdminDestinationController {

    private final DestinationService destinationService;

    public AdminDestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    /**
     * 创建目的地。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DestinationResponse>> create(@Valid @RequestBody CreateDestinationRequest request) {
        DestinationResponse result = destinationService.createDestination(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 更新目的地。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DestinationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDestinationRequest request) {
        DestinationResponse result = destinationService.updateDestination(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 删除目的地。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
