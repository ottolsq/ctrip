package com.ctrip.blindbox.dto;

import java.time.LocalDateTime;

/**
 * 分享响应。
 *
 * <p>生成分享链接后返回的信息，含分享码和过期时间。
 */
public record ShareResponse(
        String shareCode,
        String shareUrl,
        LocalDateTime shareExpiresAt
) {
}
