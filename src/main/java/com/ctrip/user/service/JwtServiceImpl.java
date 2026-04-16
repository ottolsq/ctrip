package com.ctrip.user.service;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.config.JwtConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 服务实现，使用 JJWT 0.12.x 完成 access token 的生成与验证。
 *
 * <h3>算法与密钥</h3>
 * <ul>
 *   <li>签名算法：HMAC-SHA256（HS256）。
 *   <li>密钥：从 {@link JwtConfig#getSecret()} 读取，通过 UTF-8 字节构造 {@link SecretKey}。
 *       密钥长度须 ≥ 32 字节（256 bit），由部署环境的 {@code JWT_SECRET} 环境变量保证。
 *   <li>{@code secretKey} 在构造时一次性初始化，之后不可变，线程安全。
 * </ul>
 *
 * <h3>Token 内容（Claims）</h3>
 * <pre>
 * {
 *   "sub": "12345",          // userId（Long 转 String）
 *   "iss": "ctrip",          // issuer
 *   "iat": 1700000000,       // 签发时间（Unix 秒）
 *   "exp": 1700000900        // 过期时间（iat + 15 分钟）
 * }
 * </pre>
 *
 * <h3>异常映射</h3>
 * <ul>
 *   <li>{@link ExpiredJwtException} → {@link TokenExpiredException}（HTTP 401，客户端可尝试刷新）
 *   <li>其他 {@link JwtException}（签名错误、格式错误等） → {@link AuthenticationException}（HTTP 401）
 * </ul>
 */
@Service
public class JwtServiceImpl implements JwtService {

    private final JwtConfig jwtConfig;
    /** 预构建的 HMAC-SHA256 密钥，构造时一次性初始化，不可变。 */
    private final SecretKey secretKey;

    public JwtServiceImpl(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Keys.hmacShaKeyFor 要求字节长度 ≥ 32；若密钥不足会在启动时立即抛出，及早暴露配置错误
        this.secretKey = Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     *
     * <p>生成的 token 格式：{@code <Base64Header>.<Base64Payload>.<Signature>}
     */
    @Override
    public String generateAccessToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))          // sub = userId
                .issuer(jwtConfig.getIssuer())            // iss = "ctrip"
                .issuedAt(new Date(now))                  // iat
                .expiration(new Date(now + jwtConfig.getAccessTokenExpirationMs())) // exp
                .signWith(secretKey)                      // HS256
                .compact();
    }

    /**
     * {@inheritDoc}
     *
     * @throws TokenExpiredException   token 过期
     * @throws AuthenticationException token 无效（签名错误、格式错误等）
     */
    @Override
    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * {@inheritDoc}
     *
     * <p>捕获所有异常并返回 {@code false}，不向调用方抛出，
     * 适合在过滤器中用于快速判断是否需要进一步处理请求。
     */
    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (TokenExpiredException | AuthenticationException e) {
            return false;
        }
    }

    // ── 私有辅助 ─────────────────────────────────────────────────────────────

    /**
     * 解析并验证 JWT，返回 Claims。
     *
     * <p>统一在此处完成异常转换：JJWT 异常 → 项目自定义异常。
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)   // 设置验证密钥（JJWT 0.12.x API）
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // token 格式合法但已过期，客户端可尝试用 refresh token 续期
            throw new TokenExpiredException("access token 已过期");
        } catch (JwtException e) {
            // 签名错误、格式错误、issuer 不符等，统一视为认证失败
            throw new AuthenticationException("无效的 access token");
        }
    }
}
