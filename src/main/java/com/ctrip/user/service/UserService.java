package com.ctrip.user.service;

import com.ctrip.user.dto.request.ChangePasswordRequest;
import com.ctrip.user.dto.request.UpdateProfileRequest;
import com.ctrip.user.dto.response.UserProfileResponse;

/**
 * 用户资料服务接口，负责已认证用户的个人信息管理。
 */
public interface UserService {

    /**
     * 获取用户资料。
     *
     * @param userId 当前登录用户 ID
     * @return 用户资料 DTO
     * @throws com.ctrip.common.exception.ResourceNotFoundException 用户不存在时
     */
    UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户资料（所有字段可选，null 表示不修改）。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求（username/avatarUrl/gender/birthday/realName）
     * @return 更新后的用户资料 DTO
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码（需提供当前密码进行二次确认）。
     *
     * @param userId  当前登录用户 ID
     * @param request 包含 currentPassword 和 newPassword 的请求
     * @throws com.ctrip.common.exception.AuthenticationException 当前密码验证失败时
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 更新头像 URL。
     *
     * @param userId    当前登录用户 ID
     * @param avatarUrl 新头像地址
     * @return 更新后的用户资料 DTO
     */
    UserProfileResponse updateAvatar(Long userId, String avatarUrl);
}
