package com.ctrip.user.converter;

import com.ctrip.user.dto.response.AdminUserDetailResponse;
import com.ctrip.user.dto.response.AdminUserResponse;
import com.ctrip.user.entity.User;

/**
 * 管理端用户 DTO 转换器。显式映射，禁止暴露密码哈希。
 */
public final class AdminUserConverter {

    private AdminUserConverter() {
    }

    public static AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    public static AdminUserDetailResponse toDetailResponse(User user,
                                                           long guideCount,
                                                           long orderCount) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getStatusBeforeSuspend() != null
                        ? user.getStatusBeforeSuspend().name() : null,
                guideCount,
                orderCount,
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
