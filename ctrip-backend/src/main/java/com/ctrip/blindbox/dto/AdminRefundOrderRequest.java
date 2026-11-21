package com.ctrip.blindbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端订单退款请求。
 */
public record AdminRefundOrderRequest(
        @NotBlank(message = "退款原因不能为空")
        @Size(max = 500, message = "退款原因不能超过 500 字")
        String reason
) {
}
