package com.ctrip.operation.dto;

import java.time.LocalDateTime;

/**
 * 轮播图响应。
 */
public record BannerResponse(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sortOrder,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt
) {
}
