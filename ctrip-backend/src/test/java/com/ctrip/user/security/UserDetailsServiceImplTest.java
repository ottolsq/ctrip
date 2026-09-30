package com.ctrip.user.security;

import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.mapper.UserMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link UserDetailsServiceImpl} 单元测试。
 *
 * <p>使用 Mockito 模拟 {@link UserMapper}，不启动 Spring 容器。
 * 覆盖场景：各状态用户的加载行为、非法 userId 格式、用户不存在。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl 单元测试")
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userMapper);
    }

    // ── 正常加载场景 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ACTIVE 用户：返回 enabled UserDetails，username = userId 字符串，password = passwordHash")
    void loadUserByUsername_activeUser_returnsEnabledUserDetails() {
        User user = buildUser(1L, "bcrypt_hashed_password", UserStatus.ACTIVE);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserDetails details = userDetailsService.loadUserByUsername("1");

        assertThat(details.getUsername()).isEqualTo("1");
        assertThat(details.getPassword()).isEqualTo("bcrypt_hashed_password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("UNVERIFIED 用户：正常返回（服务层负责细粒度权限控制）")
    void loadUserByUsername_unverifiedUser_returnsEnabledUserDetails() {
        User user = buildUser(2L, "bcrypt_hashed_password", UserStatus.UNVERIFIED);
        when(userMapper.selectById(2L)).thenReturn(user);

        UserDetails details = userDetailsService.loadUserByUsername("2");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("SUSPENDED 用户：disabled=true 且 accountLocked=true（DaoAuthenticationProvider 会拦截登录）")
    void loadUserByUsername_suspendedUser_returnsDisabledAndLockedUserDetails() {
        User user = buildUser(3L, "bcrypt_hashed_password", UserStatus.SUSPENDED);
        when(userMapper.selectById(3L)).thenReturn(user);

        UserDetails details = userDetailsService.loadUserByUsername("3");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    // ── 异常场景 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETED 用户：抛出 UsernameNotFoundException（对外等同于不存在，防止账号状态枚举）")
    void loadUserByUsername_deletedUser_throwsUsernameNotFoundException() {
        User user = buildUser(4L, "bcrypt_hashed_password", UserStatus.DELETED);
        when(userMapper.selectById(4L)).thenReturn(user);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("4"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("用户不存在（Mapper 返回 null）：抛出 UsernameNotFoundException")
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("99"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("userId 非数字（非法格式）：抛出 UsernameNotFoundException")
    void loadUserByUsername_nonNumericId_throwsUsernameNotFoundException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("not-a-number"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    /**
     * 构建仅填充必要字段的 User 实体（用于测试，其余字段保持 null）。
     */
    private User buildUser(Long id, String passwordHash, UserStatus status) {
        return User.builder()
                .id(id)
                .username("user_" + id)
                .passwordHash(passwordHash)
                .status(status)
                .build();
    }
}
