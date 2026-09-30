package com.ctrip.user.converter;

import com.ctrip.user.dto.response.UserProfileResponse;
import com.ctrip.user.entity.User;

/**
 * 用户模块 Entity ↔ DTO 转换器。
 *
 * <p>设计约定：
 * <ul>
 *   <li>仅包含静态方法，无状态，不注册为 Spring Bean。</li>
 *   <li>转换逻辑集中在此类，Service 层不直接操作字段映射。</li>
 * </ul>
 */
public class UserConverter {

    private UserConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将 {@link User} entity 转换为 {@link UserProfileResponse} DTO。
     *
     * @param user 用户实体（不能为 null）
     * @return 用户资料响应 DTO
     */
    public static UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getBirthday(),
                user.getRealName(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                Boolean.TRUE.equals(user.getPhoneVerified()),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getRole() != null ? user.getRole().name() : null,
                user.getCreatedAt()
        );
    }
}
