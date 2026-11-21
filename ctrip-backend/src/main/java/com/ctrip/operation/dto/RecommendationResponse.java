package com.ctrip.operation.dto;

import java.time.LocalDateTime;

/**
 * 管理端推荐位响应。
 */
public record RecommendationResponse(
        Long id,
        String slot,
        String title,
        String targetType,
        Long targetId,
        Integer sortOrder,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt
) {
}
