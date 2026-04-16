package com.ctrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置（骨架版）。
 *
 * <p>当前阶段目标：让应用能正常启动，方便对 Mapper / JwtService 等底层组件进行测试。
 * 所有请求均放行，不做任何身份验证拦截。
 *
 * <p><b>注意：此为临时配置，步骤 13 会替换为最终版（锁定路由、接入 JWT 过滤器）。</b>
 *
 * <h3>已定义的常驻 Bean</h3>
 * <ul>
 *   <li>{@link PasswordEncoder}：BCrypt 强度 12，供 {@code AuthServiceImpl}（步骤 14）注入使用。
 *       放在此处而非 AuthService 内部，避免循环依赖，也便于测试时单独注入。
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤链（骨架版）：放行所有请求。
     *
     * <p>禁用项：
     * <ul>
     *   <li>CSRF：无状态 REST API 不需要（最终版保持禁用）。
     *   <li>Session：{@code STATELESS}，认证状态完全由 JWT 承载（最终版保持不变）。
     *   <li>HttpBasic / FormLogin：API 服务不使用浏览器登录表单（最终版保持禁用）。
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF：无状态 API 不需要
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用 Session：认证状态由 JWT 承载
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 禁用 HTTP Basic 和 Form Login
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            // 骨架阶段：放行所有请求（步骤 13 替换为路由白名单）
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 密码编码器：BCrypt，强度 12。
     *
     * <p>强度 12 在现代硬件上单次哈希约 250-400 ms，可有效抵御暴力破解，
     * 同时对正常登录的响应延迟影响可接受。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
