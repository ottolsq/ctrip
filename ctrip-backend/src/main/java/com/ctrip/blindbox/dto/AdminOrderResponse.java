package com.ctrip.blindbox.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端盲盒订单列表项。
 */
public record AdminOrderResponse(
        Long id,
        String orderNo,
        Long userId,
        String userName,
        Long templateId,
        String templateName,
        BigDecimal payAmount,
        String status,
        String payMethod,
        LocalDateTime payTime,
        LocalDateTime expireAt,
        LocalDateTime createdAt,
        String refundReason,
        LocalDateTime refundedAt
) {
}
