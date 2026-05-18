package com.ctrip.content.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论树形响应 DTO。
 * 支持嵌套回复，children 存放子评论。
 */
public record CommentTreeResponse(
        Long id,
        Long userId,
        Long parentId,
        String content,
        Integer likeCount,
        LocalDateTime createdAt,
        List<CommentTreeResponse> children  // 嵌套回复
) {}
