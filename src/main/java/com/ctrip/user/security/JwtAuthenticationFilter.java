package com.ctrip.user.security;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.user.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT 认证过滤器，在每次请求时从 {@code Authorization} 请求头提取并验证 access token。
 *
 * <h3>处理逻辑</h3>
 * <ol>
 *   <li>无 {@code Authorization} 头 / 非 Bearer 格式 → 跳过，继续过滤链（白名单请求走此分支）。
 *   <li>有效 JWT → 提取 {@code userId}，写入 {@link SecurityContextHolder}，继续过滤链。
 *   <li>过期 JWT → 直接返回 HTTP 401，停止过滤链（客户端应使用 refresh token 续期）。
 *   <li>无效 JWT（签名错误、格式错误等） → 直接返回 HTTP 401，停止过滤链。
 * </ol>
 *
 * <h3>Principal 约定</h3>
 * <p>认证成功后，{@code SecurityContextHolder} 中 authentication 的 {@code principal}
 * 为 {@code Long userId}。服务层通过如下方式获取当前用户 ID：
 * <pre>{@code
 * Long userId = (Long) SecurityContextHolder.getContext()
 *                          .getAuthentication().getPrincipal();
 * }</pre>
 *
 * <h3>Bean 注册方式</h3>
 * <p>此类不标注 {@code @Component}，而是通过 {@code SecurityConfig} 中的 {@code @Bean}
 * 方法创建，避免 Spring Boot 将其自动注册为 Servlet Filter 导致的双重执行问题。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 无 Authorization 头或不是 Bearer token，跳过 JWT 验证
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractUserRole(token);

            // 仅在 SecurityContext 未设置认证时写入，防止覆盖已有认证信息
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId,                                                    // principal = Long userId
                        null,                                                      // credentials（无状态 API 不需要保留）
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))       // authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } catch (TokenExpiredException e) {
            // token 合法但已过期，客户端应使用 refresh token 续期
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (AuthenticationException e) {
            // token 签名无效、格式错误等
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 在过滤器层面直接写 JSON 错误响应。
     *
     * <p>{@link com.ctrip.common.web.GlobalExceptionHandler} 只拦截 Spring MVC 层的异常，
     * 不覆盖 Servlet Filter，因此需要在此手动序列化，格式与 {@code ApiResponse} 保持一致：
     * {@code {"success":false,"data":null,"error":"..."}}
     */
    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
