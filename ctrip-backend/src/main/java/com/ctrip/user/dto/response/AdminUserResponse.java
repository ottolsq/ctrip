package com.ctrip.user.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项。
 */
public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String avatarUrl,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}
