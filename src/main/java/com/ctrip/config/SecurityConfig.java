package com.ctrip.config;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.user.security.JwtAuthenticationFilter;
import com.ctrip.user.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 最终配置。
 *
 * <h3>路由授权规则</h3>
 * <ul>
 *   <li>{@code /api/v1/auth/**} → 无需认证（注册、登录、刷新 token、找回密码等公开端点）。
 *   <li>其他所有路径 → 必须携带有效 JWT access token。
 * </ul>
 *
 * <h3>Bean 说明</h3>
 * <ul>
 *   <li>{@link JwtAuthenticationFilter}：在此处以 {@code @Bean} 方式创建，而非在过滤器类上标注
 *       {@code @Component}，防止 Spring Boot 自动注册为 Servlet Filter 导致双重执行。
 *   <li>{@link ObjectMapper}：显式声明，因 SB 4.x webmvc starter 不含 JSON starter，
 *       {@code JacksonAutoConfiguration} 不会自动激活，过滤器需注入此 bean 序列化错误响应。
 *   <li>{@link PasswordEncoder}：BCrypt 强度 12，供 {@code AuthServiceImpl} 注入使用。
 *   <li>{@link AuthenticationManager}：暴露为 bean，供 {@code AuthServiceImpl} 可选注入，
 *       使用 Spring Security 标准认证流程（{@code DaoAuthenticationProvider}）验证用户凭据。
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * JWT 认证过滤器 bean。
     *
     * <p>以 {@code @Bean} 方式声明，Spring Boot 不会将其自动注册为 Servlet Filter，
     * 仅通过 {@link #securityFilterChain} 中的 {@code addFilterBefore} 纳入 Security 过滤链。
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtService, objectMapper);
    }

    /**
     * 安全过滤链（最终版）。
     *
     * <p>核心配置：
     * <ul>
     *   <li>禁用 CSRF / Session / HttpBasic / FormLogin（无状态 REST API）。
     *   <li>认证端点白名单放行，其余要求认证。
     *   <li>未认证 → 401 JSON；权限不足 → 403 JSON（不重定向）。
     *   <li>JWT 过滤器插入 {@link UsernamePasswordAuthenticationFilter} 之前。
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
            // 无状态 API 基础设置
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // 路由授权：认证端点公开，管理端需要 ADMIN 或 CONTENT_OPERATOR 角色，其余需要 JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/itineraries/share/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "CONTENT_OPERATOR")
                .anyRequest().authenticated()
            )

            // 异常处理：统一返回 JSON，不重定向
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    // 未认证（无 token 或 token 被 filter 拦截后未设置 SecurityContext）
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("请先登录")));
                })
                .accessDeniedHandler((request, response, e) -> {
                    // 已认证但权限不足（用户角色不满足 /api/v1/admin/** 要求）
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("权限不足")));
                })
            )

            // JWT 过滤器：在 Spring Security 的用户名密码过滤器之前执行
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Jackson ObjectMapper bean。
     *
     * <p>spring-boot-starter-webmvc（SB 4.x）不包含 spring-boot-starter-json，
     * JacksonAutoConfiguration 不会自动激活，需在此处显式声明以供 Security 过滤器使用。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
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

    /**
     * 暴露 {@link AuthenticationManager} bean。
     *
     * <p>Spring Boot 不会自动将 AuthenticationManager 暴露为 bean，需显式声明。
     * {@code AuthServiceImpl} 可注入此 bean，利用 Spring Security 标准的
     * {@code DaoAuthenticationProvider}（自动检测 {@code UserDetailsServiceImpl}）
     * 完成用户名/密码验证，也可选择直接调用 {@code UserMapper + PasswordEncoder} 手动验证。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
