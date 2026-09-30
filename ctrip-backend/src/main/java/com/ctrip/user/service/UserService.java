package com.ctrip.user.service;

import com.ctrip.user.dto.request.ChangePasswordRequest;
import com.ctrip.user.dto.request.UpdateProfileRequest;
import com.ctrip.user.dto.response.UserProfileResponse;

/**
 * 用户资料服务接口，负责已认证用户的个人信息管理。
 */
public interface UserService {

    UserProfileResponse getUserProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserProfileResponse updateAvatar(Long userId, String avatarUrl);
}
