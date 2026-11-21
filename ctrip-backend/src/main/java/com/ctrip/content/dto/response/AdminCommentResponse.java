package com.ctrip.content.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端评论列表项。
 */
public record AdminCommentResponse(
        Long id,
        Long guideId,
        String guideTitle,
        Long userId,
        String userName,
        String userAvatar,
        Long parentId,
        String content,
        LocalDateTime createdAt
) {}
