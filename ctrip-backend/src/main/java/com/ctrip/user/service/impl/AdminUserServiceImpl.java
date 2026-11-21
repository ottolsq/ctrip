package com.ctrip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.config.JwtConfig;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.user.converter.AdminUserConverter;
import com.ctrip.user.dto.response.AdminUserDetailResponse;
import com.ctrip.user.dto.response.AdminUserResponse;
import com.ctrip.user.entity.RefreshToken;
import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserRole;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.mapper.RefreshTokenMapper;
import com.ctrip.user.mapper.UserMapper;
import com.ctrip.user.security.UserBanKeys;
import com.ctrip.user.service.AdminUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 管理端用户服务实现。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final GuideMapper guideMapper;
    private final BlindBoxOrderMapper blindBoxOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtConfig jwtConfig;

    public AdminUserServiceImpl(UserMapper userMapper,
                                RefreshTokenMapper refreshTokenMapper,
                                GuideMapper guideMapper,
                                BlindBoxOrderMapper blindBoxOrderMapper,
                                StringRedisTemplate stringRedisTemplate,
                                JwtConfig jwtConfig) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.guideMapper = guideMapper;
        this.blindBoxOrderMapper = blindBoxOrderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtConfig = jwtConfig;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(int page, int size, UserStatus status, UserRole role,
                                             String keyword, LocalDate startDate, LocalDate endDate) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<User> pageObj = new Page<>(page, effectiveSize);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (startDate != null) {
            wrapper.ge(User::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(User::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(pageObj, wrapper);
        Page<AdminUserResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream()
                .map(AdminUserConverter::toResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = requireUser(userId);
        long guideCount = guideMapper.selectCount(
                new LambdaQueryWrapper<Guide>().eq(Guide::getAuthorId, userId));
        long orderCount = blindBoxOrderMapper.selectCount(
                new LambdaQueryWrapper<BlindBoxOrder>().eq(BlindBoxOrder::getUserId, userId));
        return AdminUserConverter.toDetailResponse(user, guideCount, orderCount);
    }

    @Override
    @Transactional
    public void banUser(Long operatorId, Long userId) {
        User user = requireUser(userId);
        if (operatorId.equals(userId)) {
            throw new BusinessException("不能封禁自己");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("不能封禁管理员账号");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException("已删除用户不可封禁");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            return;
        }

        User update = User.builder()
                .id(userId)
                .status(UserStatus.SUSPENDED)
                .statusBeforeSuspend(user.getStatus())
                .build();
        userMapper.updateById(update);

        refreshTokenMapper.delete(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId));

        stringRedisTemplate.opsForValue().set(
                UserBanKeys.key(userId), "1",
                Duration.ofMillis(jwtConfig.getAccessTokenExpirationMs()));
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = requireUser(userId);
        stringRedisTemplate.delete(UserBanKeys.key(userId));

        if (user.getStatus() != UserStatus.SUSPENDED) {
            return;
        }

        UserStatus restored = user.getStatusBeforeSuspend() != null
                ? user.getStatusBeforeSuspend() : UserStatus.ACTIVE;
        User update = User.builder()
                .id(userId)
                .status(restored)
                .statusBeforeSuspend(null)
                .build();
        userMapper.updateById(update);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在：id=" + userId);
        }
        return user;
    }
}
