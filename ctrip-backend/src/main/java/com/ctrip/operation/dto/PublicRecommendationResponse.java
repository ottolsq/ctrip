package com.ctrip.operation.dto;

/**
 * 公开推荐位响应：已装配目标标题、封面与跳转链接。
 */
public record PublicRecommendationResponse(
        Long id,
        String slot,
        String title,
        String targetType,
        Long targetId,
        String targetTitle,
        String coverUrl,
        String linkUrl
) {
}
