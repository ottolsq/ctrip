package com.ctrip.user.security;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.user.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JwtAuthenticationFilter} 单元测试。
 *
 * <p>使用 {@link MockHttpServletRequest}/{@link MockHttpServletResponse} 模拟 Servlet 请求，
 * Mockito 模拟 {@link JwtService} 和 {@link FilterChain}，不启动 Spring 容器。
 *
 * <p>每个测试后通过 {@link SecurityContextHolder#clearContext()} 清理认证状态，
 * 防止测试间状态泄漏。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 单元测试")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        // 防止测试之间的 SecurityContext 认证状态泄漏
        SecurityContextHolder.clearContext();
    }

    // ── 跳过 JWT 验证场景（直接继续过滤链）──────────────────────────────────

    @Test
    @DisplayName("无 Authorization 请求头：跳过 JWT 验证，继续过滤链")
    void noAuthorizationHeader_shouldContinueFilterChain() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization 头为 Basic 格式（非 Bearer）：跳过 JWT 验证，继续过滤链")
    void basicAuthorizationHeader_shouldContinueFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── 有效 JWT 场景 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("有效 JWT：将 userId 写入 SecurityContext（principal = Long），继续过滤链")
    void validToken_shouldSetSecurityContextAndContinueFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(42L);

        filter.doFilterInternal(request, response, filterChain);

        // 过滤链应继续
        verify(filterChain).doFilter(request, response);
        // HTTP 状态码维持 200（未被过滤器修改）
        assertThat(response.getStatus()).isEqualTo(200);
        // SecurityContext 中已设置认证，principal 为 Long userId
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
    }

    // ── JWT 异常场景（停止过滤链，直接写 401 响应）──────────────────────────

    @Test
    @DisplayName("过期 JWT：返回 401 JSON 错误响应，过滤链不继续")
    void expiredToken_shouldReturn401AndStopFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        when(jwtService.extractUserId("expired.jwt.token"))
                .thenThrow(new TokenExpiredException("access token 已过期"));

        filter.doFilterInternal(request, response, filterChain);

        // 过滤链必须被阻断
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString();
        assertThat(body).contains("\"success\":false");
        assertThat(body).contains("access token 已过期");
    }

    @Test
    @DisplayName("签名无效 JWT（篡改/错误密钥）：返回 401 JSON 错误响应，过滤链不继续")
    void invalidToken_shouldReturn401AndStopFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        when(jwtService.extractUserId("invalid.jwt.token"))
                .thenThrow(new AuthenticationException("无效的 access token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString();
        assertThat(body).contains("\"success\":false");
        assertThat(body).contains("无效的 access token");
    }
}
