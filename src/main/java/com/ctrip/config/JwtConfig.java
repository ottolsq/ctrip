package com.ctrip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，绑定 application.properties 中 {@code app.jwt.*} 前缀的配置项。
 *
 * <h3>配置示例（application.properties）</h3>
 * <pre>
 * app.jwt.secret=${JWT_SECRET}                         # 签名密钥（环境变量注入）
 * app.jwt.access-token-expiration-ms=900000            # access token 有效期 15 分钟
 * app.jwt.refresh-token-expiration-ms=1296000000       # refresh token 有效期 15 天
 * app.jwt.issuer=ctrip                                 # JWT iss 字段
 * </pre>
 *
 * <h3>安全注意</h3>
 * <ul>
 *   <li>{@code secret} 必须通过环境变量 {@code JWT_SECRET} 注入，禁止硬编码在配置文件中。
 *   <li>密钥长度建议 ≥ 256 bit（32 字节），确保 HMAC-SHA256 的安全强度。
 * </ul>
 *
 * <p>{@link Component} 使 Spring 可以将此 bean 注入到需要的地方（如 {@code JwtServiceImpl}）。
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /** HMAC-SHA256 签名密钥（Base64 或原始字节均可，从环境变量读取）。 */
    private String secret;

    /** Access token 有效期，单位毫秒。默认 900_000 ms = 15 分钟。 */
    private long accessTokenExpirationMs;

    /** Refresh token 有效期，单位毫秒。默认 1_296_000_000 ms = 15 天。 */
    private long refreshTokenExpirationMs;

    /** JWT iss（issuer）声明值，用于验证 token 来源。 */
    private String issuer;

    // ── Getters / Setters ────────────────────────────────────────────────────
    // Spring Boot @ConfigurationProperties 通过 setter 绑定，不能省略。

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
