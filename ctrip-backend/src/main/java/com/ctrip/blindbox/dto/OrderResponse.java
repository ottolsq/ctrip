package com.ctrip.blindbox.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应。
 *
 * <p>创建订单后返回给前端的信息，含订单号、金额、过期时间等。
 */
public record OrderResponse(
        Long id,
        String orderNo,
        Long templateId,
        String templateName,
        BigDecimal payAmount,
        String status,
        LocalDateTime expireAt,
        LocalDateTime payTime,
        LocalDateTime createdAt
) {
}
