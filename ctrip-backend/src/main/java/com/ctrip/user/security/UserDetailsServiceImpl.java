package com.ctrip.user.security;

import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.mapper.UserMapper;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security {@link UserDetailsService} 实现。
 *
 * <h3>调用时机</h3>
 * <ul>
 *   <li>由 Spring Security 的 {@code DaoAuthenticationProvider} 在验证用户凭据时调用。
 *   <li>传入的 {@code userId} 是 JWT {@code sub} 字段中存储的用户 ID（字符串形式），
 *       由 {@code JwtAuthenticationFilter} 提取后传入（或由 AuthenticationManager 使用）。
 * </ul>
 *
 * <h3>为何提供此 Bean</h3>
 * <p>Spring Boot 会检测 classpath 上的 Spring Security，若未发现任何 {@link UserDetailsService} bean，
 * 则自动生成一个随机密码的内存用户（{@code UserDetailsServiceAutoConfiguration}）并打印警告日志。
 * 提供此实现可阻止该默认行为，并接管认证数据源。
 *
 * <h3>状态处理规则</h3>
 * <ul>
 *   <li>{@code ACTIVE} / {@code UNVERIFIED}：正常返回，服务层按需做更细粒度的业务权限控制。
 *   <li>{@code SUSPENDED}：返回 {@code disabled=true}，{@code AuthenticationManager.authenticate()}
 *       将抛出 {@code DisabledException}，由 {@code AuthServiceImpl} 捕获并转换为业务异常。
 *   <li>{@code DELETED}：抛出 {@code UsernameNotFoundException}，等同于用户不存在，
 *       避免向外部暴露账号已被删除的信息。
 * </ul>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按用户 ID 加载用户详情。
     *
     * @param userId 字符串形式的用户 ID（来自 JWT sub 字段）
     * @return Spring Security {@link UserDetails}，包含 id、passwordHash、账号状态
     * @throws UsernameNotFoundException 用户不存在或已被软删除
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            // 传入的标识不是合法数字，视为用户不存在
            throw new UsernameNotFoundException("用户不存在");
        }

        User user = userMapper.selectById(id);

        // DELETED 状态视同不存在，对外统一返回"用户不存在"，避免枚举账号状态
        if (user == null || user.getStatus() == UserStatus.DELETED) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 使用 Spring Security 内置 User builder 构造 UserDetails
        // authorities 传空列表：当前系统暂无角色体系，权限控制由服务层负责
        return org.springframework.security.core.userdetails.User
                .withUsername(String.valueOf(user.getId()))
                .password(user.getPasswordHash())
                .disabled(user.getStatus() == UserStatus.SUSPENDED)
                .accountLocked(user.getStatus() == UserStatus.SUSPENDED)
                .authorities(Collections.emptyList())
                .build();
    }
}
