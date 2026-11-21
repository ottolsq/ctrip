package com.ctrip.content.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端攻略审核列表项。
 */
public record AdminGuideListItemResponse(
        Long id,
        Long authorId,
        String authorName,
        String title,
        String status,
        String rejectReason,
        Integer viewCount,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
