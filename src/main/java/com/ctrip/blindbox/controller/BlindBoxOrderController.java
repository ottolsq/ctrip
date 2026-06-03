package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.CreateOrderRequest;
import com.ctrip.blindbox.dto.OrderResponse;
import com.ctrip.blindbox.dto.ResultResponse;
import com.ctrip.blindbox.dto.ShareResponse;
import com.ctrip.blindbox.service.BlindBoxOrderService;
import com.ctrip.blindbox.service.BlindBoxResultService;
import com.ctrip.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 盲盒订单控制器。
 *
 * <p>路径前缀：/api/v1/blind-box/orders
 * 所有端点需要 JWT 认证。
 */
@RestController
@RequestMapping("/api/v1/blind-box/orders")
public class BlindBoxOrderController {

    private final BlindBoxOrderService orderService;
    private final BlindBoxResultService resultService;

    public BlindBoxOrderController(BlindBoxOrderService orderService,
                                   BlindBoxResultService resultService) {
        this.orderService = orderService;
        this.resultService = resultService;
    }

    /**
     * 创建订单。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse result = orderService.createOrder(userId, request);
        return ResponseEntity.status(201).body(ApiResponse.ok(result));
    }

    /**
     * 我的订单列表（分页）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listMyOrders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        Page<OrderResponse> result = orderService.listMyOrders(userId, page, limit, status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 订单详情。
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        OrderResponse result = orderService.getOrderDetail(userId, orderNo);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 取消订单。
     */
    @PostMapping("/{orderNo}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        orderService.cancelOrder(userId, orderNo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 开盒。
     */
    @PostMapping("/{orderNo}/open")
    public ResponseEntity<ApiResponse<OrderResponse>> openBox(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        OrderResponse result = orderService.openBox(userId, orderNo);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 结果详情。
     */
    @GetMapping("/{orderNo}/result")
    public ResponseEntity<ApiResponse<ResultResponse>> getResult(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        ResultResponse result = resultService.getResult(userId, orderNo);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 生成分享链接。
     */
    @PostMapping("/{orderNo}/share-result")
    public ResponseEntity<ApiResponse<ShareResponse>> generateShareLink(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        ShareResponse result = resultService.generateShareLink(userId, orderNo);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
