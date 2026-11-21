package com.ctrip.user.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端用户详情。
 */
public record AdminUserDetailResponse(
        Long id,
        String username,
        String email,
        String phone,
        String avatarUrl,
        String role,
        String status,
        String statusBeforeSuspend,
        long guideCount,
        long orderCount,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}
