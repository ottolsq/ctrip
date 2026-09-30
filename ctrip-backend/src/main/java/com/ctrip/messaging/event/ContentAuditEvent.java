package com.ctrip.messaging.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 内容审核事件。
 *
 * <p>触发时机：攻略提交审核、审核通过、审核拒绝。
 * Exchange: content.audit.events
 */
public record ContentAuditEvent(
        String eventId,
        String eventType,
        Long guideId,
        Long authorId,
        Long operatorId,
        String reason,
        OffsetDateTime timestamp
) {

    /**
     * 构建内容审核事件（自动生成 eventId 和 timestamp）。
     */
    public static ContentAuditEvent of(String eventType, Long guideId, Long authorId,
                                       Long operatorId, String reason) {
        return new ContentAuditEvent(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                eventType,
                guideId,
                authorId,
                operatorId,
                reason,
                OffsetDateTime.now()
        );
    }
}
