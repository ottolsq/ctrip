package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.OrderResponse;
import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.service.impl.BlindBoxTemplateServiceImpl;
import com.ctrip.blindbox.service.BlindBoxOrderService;
import com.ctrip.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台盲盒管理控制器。
 *
 * <p>路径前缀：/api/v1/admin/blind-box
 * 所有端点需要 JWT + ADMIN 角色。
 */
@RestController
@RequestMapping("/api/v1/admin/blind-box")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlindBoxController {

    private final BlindBoxTemplateServiceImpl templateService;
    private final BlindBoxOrderService orderService;

    public AdminBlindBoxController(BlindBoxTemplateServiceImpl templateService,
                                   BlindBoxOrderService orderService) {
        this.templateService = templateService;
        this.orderService = orderService;
    }

    // ========== 模板管理 ==========

    /**
     * 模板列表（分页）。
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Page<TemplateResponse> result = templateService.listAllTemplates(page, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 创建模板。
     */
    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @RequestBody BlindBoxTemplate template) {
        TemplateResponse result = templateService.createTemplate(template);
        return ResponseEntity.status(201).body(ApiResponse.ok(result));
    }

    /**
     * 更新模板。
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @RequestBody BlindBoxTemplate template) {
        TemplateResponse result = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 删除模板。
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ========== 订单管理 ==========

    /**
     * 全部订单列表（分页）。
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        // MVP 阶段返回空分页结果，后续可扩展 listAllOrders 方法
        return ResponseEntity.ok(ApiResponse.ok(new Page<>(page, limit)));
    }
}
