package com.ctrip.user.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.user.converter.UserConverter;
import com.ctrip.user.dto.request.ChangePasswordRequest;
import com.ctrip.user.dto.request.UpdateProfileRequest;
import com.ctrip.user.dto.response.UserProfileResponse;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户资料服务实现类，处理已认证用户的个人信息管理。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取用户资料。
     * 按 userId 查询用户，不存在则抛出 ResourceNotFoundException。
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = requireUser(userId);
        return UserConverter.toProfileResponse(user);
    }

    /**
     * 更新用户资料（只更新请求中非 null 的字段）。
     * 写操作加 @Transactional，更新后重新查询返回最新状态。
     */
    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        requireUser(userId);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);

        if (request.username() != null) wrapper.set(User::getUsername, request.username());
        if (request.avatarUrl() != null) wrapper.set(User::getAvatarUrl, request.avatarUrl());
        if (request.gender() != null)    wrapper.set(User::getGender, request.gender());
        if (request.birthday() != null)  wrapper.set(User::getBirthday, request.birthday());
        if (request.realName() != null)  wrapper.set(User::getRealName, request.realName());

        // 若所有字段均为 null，跳过 SQL 避免无效触发 updatedAt
        boolean hasUpdate = request.username() != null || request.avatarUrl() != null
                || request.gender() != null || request.birthday() != null || request.realName() != null;
        if (hasUpdate) {
            userMapper.update(null, wrapper);
        }

        // 重新查询以返回最新数据（无论是否有变更，统一从 DB 返回最新状态）
        return UserConverter.toProfileResponse(requireUser(userId));
    }

    /**
     * 修改密码：先验证当前密码，通过后更新为新密码哈希。
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("当前密码不正确");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(request.newPassword())));
    }

    /**
     * 更新头像 URL。
     */
    @Override
    @Transactional
    public UserProfileResponse updateAvatar(Long userId, String avatarUrl) {
        requireUser(userId);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAvatarUrl, avatarUrl));

        return UserConverter.toProfileResponse(requireUser(userId));
    }

    /**
     * 查询用户，不存在则抛出 ResourceNotFoundException（HTTP 404）。
     *
     * @param userId 用户 ID
     * @return 非 null 的用户实体
     */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在：id=" + userId);
        }
        return user;
    }
}
