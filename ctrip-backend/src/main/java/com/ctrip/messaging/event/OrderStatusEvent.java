package com.ctrip.messaging.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 订单状态事件。
 *
 * <p>触发时机：盲盒订单创建、支付回调确认、开盒完成、退款、取消。
 * Exchange: order.status.events
 */
public record OrderStatusEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNo,
        Long userId,
        Long templateId,
        BigDecimal payAmount,
        String payMethod,
        OffsetDateTime timestamp,
        Map<String, Object> metadata
) {

    /**
     * 构建订单状态事件（自动生成 eventId 和 timestamp）。
     */
    public static OrderStatusEvent of(String eventType, Long orderId, String orderNo,
                                      Long userId, Long templateId, BigDecimal payAmount,
                                      String payMethod, Map<String, Object> metadata) {
        return new OrderStatusEvent(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                eventType,
                orderId,
                orderNo,
                userId,
                templateId,
                payAmount,
                payMethod,
                OffsetDateTime.now(),
                metadata
        );
    }
}
