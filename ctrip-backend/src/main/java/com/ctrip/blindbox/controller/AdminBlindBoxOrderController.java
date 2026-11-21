package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.AdminOrderDetailResponse;
import com.ctrip.blindbox.dto.AdminOrderResponse;
import com.ctrip.blindbox.dto.AdminRefundOrderRequest;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.service.AdminBlindBoxOrderService;
import com.ctrip.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/**
 * 管理端盲盒订单控制器：列表、详情、退款、异常重试。
 */
@RestController
@RequestMapping("/api/v1/admin/blind-box/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlindBoxOrderController {

    private final AdminBlindBoxOrderService adminOrderService;

    public AdminBlindBoxOrderController(AdminBlindBoxOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminOrderResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Page<AdminOrderResponse> result = adminOrderService.listOrders(
                page, size, orderNo, userId, status, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> detail(@PathVariable String orderNo) {
        return ResponseEntity.ok(ApiResponse.ok(adminOrderService.getOrderDetail(orderNo)));
    }

    @PostMapping("/{orderNo}/refund")
    public ResponseEntity<ApiResponse<Void>> refund(
            @PathVariable String orderNo,
            @Valid @RequestBody AdminRefundOrderRequest request,
            @AuthenticationPrincipal Long operatorId) {
        adminOrderService.refund(orderNo, request.reason(), operatorId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{orderNo}/retry-open")
    public ResponseEntity<ApiResponse<Void>> retryOpen(@PathVariable String orderNo) {
        adminOrderService.retryOpen(orderNo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
