package com.ctrip.messaging.event;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 盲盒开盒事件。
 *
 * <p>触发时机：用户点击开盒。
 * Exchange: blindbox.open.events
 */
public record BlindBoxOpenEvent(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        Long resultId,
        OffsetDateTime timestamp,
        Map<String, Object> metadata
) {

    /**
     * 构建盲盒开盒事件（自动生成 eventId 和 timestamp）。
     */
    public static BlindBoxOpenEvent of(String eventType, Long orderId, Long userId,
                                       Long resultId, Map<String, Object> metadata) {
        return new BlindBoxOpenEvent(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                eventType,
                orderId,
                userId,
                resultId,
                OffsetDateTime.now(),
                metadata
        );
    }
}
