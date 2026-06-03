package com.ctrip.blindbox.dto;

import java.time.LocalDateTime;

/**
 * 盲盒结果响应。
 *
 * <p>开盒后返回的旅行方案，包含目的地、主题、完整行程 JSON 等。
 * shareCode 为分享短码，用于生成公开分享链接。
 */
public record ResultResponse(
        Long id,
        String orderNo,
        String destination,
        Long destinationId,
        String theme,
        String resultText,
        String shareCode,
        LocalDateTime openedAt,
        LocalDateTime createdAt
) {
}
