package com.ctrip.content.dto.response;

import java.time.LocalDateTime;

/**
 * 攻略列表项响应 DTO。
 * 与 GuideResponse 相比，不包含 content 正文（列表页不需要）。
 */
public record GuideListResponse(
        Long id,
        Long authorId,
        String authorName,
        String authorAvatar,
        String title,
        String coverUrl,
        String destinationName,    // 关联目的地名称（可选）
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt
) {}
