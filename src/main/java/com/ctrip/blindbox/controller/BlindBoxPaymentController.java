package com.ctrip.blindbox.controller;

import com.ctrip.blindbox.service.BlindBoxOrderService;
import com.ctrip.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 盲盒模拟支付回调控制器。
 *
 * <p>路径：/api/v1/payments/blind-box/callback
 * MVP 阶段使用，不对接真实支付渠道，无需认证。
 */
@RestController
@RequestMapping("/api/v1/payments/blind-box")
public class BlindBoxPaymentController {

    private final BlindBoxOrderService orderService;

    public BlindBoxPaymentController(BlindBoxOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 模拟支付回调。
     *
     * <p>请求体：{"orderNo": "BB202606020001", "payMethod": "ALIPAY"}
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> payCallback(@RequestBody Map<String, String> request) {
        String orderNo = request.get("orderNo");
        String payMethod = request.get("payMethod");

        orderService.payCallback(orderNo, payMethod);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
