package com.ctrip.messaging.event;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 用户行为事件。
 *
 * <p>触发时机：用户注册、浏览攻略、点赞、创建行程、购买盲盒、分享行程等。
 * Exchange: user.activity.events
 */
public record UserActivityEvent(
        String eventId,
        String eventType,
        Long userId,
        Long targetId,
        String targetType,
        OffsetDateTime timestamp,
        Map<String, Object> metadata
) {

    /**
     * 构建用户行为事件（自动生成 eventId 和 timestamp）。
     */
    public static UserActivityEvent of(String eventType, Long userId, Long targetId,
                                       String targetType, Map<String, Object> metadata) {
        return new UserActivityEvent(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                eventType,
                userId,
                targetId,
                targetType,
                OffsetDateTime.now(),
                metadata
        );
    }
}
