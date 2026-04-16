package com.ctrip.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO（步骤 15 UserServiceImpl / UserController 使用）。
 *
 * <p>Entity → DTO 转换由 {@code UserConverter} 完成，此类不含业务逻辑。
 */
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String phone,
        String avatarUrl,
        String gender,
        LocalDate birthday,
        String realName,
        boolean emailVerified,
        boolean phoneVerified,
        String status,
        LocalDateTime createdAt
) {}
