package com.ctrip.user.service;

/**
 * JWT 操作接口，定义 access token 的生成与验证能力。
 *
 * <h3>职责边界</h3>
 * <ul>
 *   <li>此接口只处理 JWT（access token）；refresh token 的生成、存储和轮换由
 *       {@code AuthService} 负责，因为 refresh token 需要写数据库。
 *   <li>接口依赖 {@link com.ctrip.config.JwtConfig} 中的配置，实现类通过构造器注入。
 * </ul>
 *
 * <h3>Access Token 设计</h3>
 * <ul>
 *   <li>算法：HMAC-SHA256（HS256），密钥来自环境变量 {@code JWT_SECRET}。
 *   <li>Claims：{@code sub} = userId（字符串形式），{@code iss} = "ctrip"，{@code exp} = 过期时间。
 *   <li>有效期：15 分钟（见 {@code app.jwt.access-token-expiration-ms}）。
 * </ul>
 */
public interface JwtService {

    /**
     * 为指定用户生成 access token。
     *
     * @param userId 用户 ID，将作为 JWT {@code sub} 字段写入
     * @return 签名后的 JWT 字符串（Header.Payload.Signature 格式）
     */
    String generateAccessToken(Long userId);

    /**
     * 验证 token 合法性并提取 userId。
     *
     * @param token JWT 字符串（不含 "Bearer " 前缀）
     * @return token 中的 userId
     * @throws com.ctrip.common.exception.TokenExpiredException   token 已过期
     * @throws com.ctrip.common.exception.AuthenticationException token 签名无效或格式错误
     */
    Long extractUserId(String token);

    /**
     * 仅检查 token 是否有效（不抛异常版本，用于过滤器快速判断）。
     *
     * @param token JWT 字符串
     * @return {@code true} 表示 token 合法且未过期
     */
    boolean isTokenValid(String token);

    /**
     * 返回 access token 有效期秒数，供 TokenResponse 使用。
     *
     * @return access token 有效期（秒）
     */
    long getAccessTokenExpiresInSeconds();

    /**
     * 计算并返回新 refresh token 的过期时间点。
     * 封装时间计算，使 AuthService 不需要直接依赖 JwtConfig。
     *
     * @return refresh token 过期时间（LocalDateTime）
     */
    java.time.LocalDateTime calculateRefreshTokenExpiry();
}
