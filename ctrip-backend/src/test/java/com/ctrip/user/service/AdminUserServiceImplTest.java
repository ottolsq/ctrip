package com.ctrip.user.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.config.JwtConfig;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.user.dto.response.AdminUserDetailResponse;
import com.ctrip.user.dto.response.AdminUserResponse;
import com.ctrip.user.entity.RefreshToken;
import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserRole;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.mapper.RefreshTokenMapper;
import com.ctrip.user.mapper.UserMapper;
import com.ctrip.user.security.UserBanKeys;
import com.ctrip.user.service.impl.AdminUserServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("后台用户服务单元测试")
class AdminUserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private BlindBoxOrderMapper blindBoxOrderMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AdminUserServiceImpl adminUserService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, RefreshToken.class);
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
    }

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAccessTokenExpirationMs(900_000L);
        adminUserService = new AdminUserServiceImpl(
                userMapper, refreshTokenMapper, guideMapper, blindBoxOrderMapper,
                stringRedisTemplate, jwtConfig);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private User user(Long id, UserRole role, UserStatus status) {
        return User.builder()
                .id(id).username("user" + id).email("user" + id + "@example.com")
                .phone("1380000000" + id).avatarUrl("https://img.example.com/" + id + ".jpg")
                .role(role).status(status).createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("用户列表：分页映射且不暴露密码")
    void listUsers_shouldMapPageAndFilters() {
        User user = user(2L, UserRole.USER, UserStatus.ACTIVE);
        when(userMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1L);
            return page;
        });

        Page<AdminUserResponse> result = adminUserService.listUsers(
                1, 10, UserStatus.ACTIVE, UserRole.USER, "user", null, null);

        assertThat(result.getRecords()).hasSize(1);
        AdminUserResponse response = result.getRecords().get(0);
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.username()).isEqualTo("user2");
        assertThat(response.email()).isEqualTo("user2@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("用户详情：包含攻略数与订单数")
    void getUserDetail_shouldIncludeGuideAndOrderCounts() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, UserRole.USER, UserStatus.ACTIVE));
        when(guideMapper.selectCount(any())).thenReturn(3L);
        when(blindBoxOrderMapper.selectCount(any())).thenReturn(5L);

        AdminUserDetailResponse detail = adminUserService.getUserDetail(2L);

        assertThat(detail.id()).isEqualTo(2L);
        assertThat(detail.guideCount()).isEqualTo(3L);
        assertThat(detail.orderCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("封禁普通用户：记录原状态、吊销刷新令牌、写入 Redis")
    void banUser_activeUser_shouldStoreStatusSuspendAndSetDenyKey() {
        User target = user(2L, UserRole.USER, UserStatus.ACTIVE);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.banUser(1L, 2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(captor.getValue().getStatusBeforeSuspend()).isEqualTo(UserStatus.ACTIVE);
        verify(refreshTokenMapper).delete(any());
        verify(valueOperations).set(UserBanKeys.key(2L), "1", Duration.ofMillis(900_000L));
    }

    @Test
    @DisplayName("封禁自己：拒绝")
    void banUser_self_shouldThrow() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, UserRole.USER, UserStatus.ACTIVE));

        assertThatThrownBy(() -> adminUserService.banUser(2L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能封禁自己");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("封禁管理员账号：拒绝")
    void banUser_adminTarget_shouldThrow() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, UserRole.ADMIN, UserStatus.ACTIVE));

        assertThatThrownBy(() -> adminUserService.banUser(1L, 2L))
                .isInstanceOf(BusinessException.class);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("重复封禁：不覆盖原状态")
    void banUser_alreadySuspended_shouldNotOverwriteStatusBefore() {
        User target = user(2L, UserRole.USER, UserStatus.SUSPENDED);
        target.setStatusBeforeSuspend(UserStatus.UNVERIFIED);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.banUser(1L, 2L);

        verify(userMapper, never()).updateById(any(User.class));
        verify(refreshTokenMapper, never()).delete(any());
    }

    @Test
    @DisplayName("解封：恢复封禁前状态并删除 Redis 封禁键")
    void unbanUser_suspended_shouldRestoreStatusBeforeSuspend() {
        User target = user(2L, UserRole.USER, UserStatus.SUSPENDED);
        target.setStatusBeforeSuspend(UserStatus.UNVERIFIED);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.unbanUser(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.UNVERIFIED);
        assertThat(captor.getValue().getStatusBeforeSuspend()).isNull();
        verify(stringRedisTemplate).delete(UserBanKeys.key(2L));
    }

    @Test
    @DisplayName("解封且无历史状态：恢复为 ACTIVE")
    void unbanUser_withoutPreviousStatus_shouldRestoreActive() {
        User target = user(2L, UserRole.USER, UserStatus.SUSPENDED);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.unbanUser(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(captor.getValue().getStatusBeforeSuspend()).isNull();
    }

    @Test
    @DisplayName("解封未封禁用户：幂等且清理 Redis 键")
    void unbanUser_notSuspended_shouldBeIdempotent() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, UserRole.USER, UserStatus.ACTIVE));

        adminUserService.unbanUser(2L);

        verify(userMapper, never()).updateById(any(User.class));
        verify(stringRedisTemplate).delete(UserBanKeys.key(2L));
    }
}
