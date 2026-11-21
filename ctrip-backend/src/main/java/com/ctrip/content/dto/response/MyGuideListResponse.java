package com.ctrip.content.dto.response;

import java.time.LocalDateTime;

/**
 * 作者“我的攻略”列表项（包含非公开状态与驳回理由）。
 */
public record MyGuideListResponse(
        Long id,
        Long authorId,
        String title,
        String coverUrl,
        String destinationName,
        String status,
        String rejectReason,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
