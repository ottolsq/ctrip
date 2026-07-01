package com.ctrip.content.dto.response;

import java.time.LocalDateTime;

/**
 * 攻略详情响应 DTO。
 * 包含完整的攻略信息（含 content 正文）。
 */
public record GuideResponse(
        Long id,
        Long authorId,
        String authorName,
        String authorAvatar,
        String title,
        String content,
        Long destinationId,
        String destinationName,    // 关联目的地名称（可选）
        String coverUrl,
        String imageUrls,
        String status,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
