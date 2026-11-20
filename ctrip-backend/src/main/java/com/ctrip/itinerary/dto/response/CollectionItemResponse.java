package com.ctrip.itinerary.dto.response;

import java.time.LocalDateTime;

/**
 * 我的收藏列表项响应。
 *
 * <p>统一描述攻略/行程收藏的展示所需字段。
 */
public record CollectionItemResponse(
        Long id,
        String contentType,
        Long contentId,
        String title,
        String coverUrl,
        String summary,
        String authorName,
        LocalDateTime createdAt
) {}
