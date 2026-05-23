package com.ctrip.user.service;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.config.JwtConfig;
import com.ctrip.user.entity.enums.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JwtServiceImpl} 单元测试。
 *
 * <p>不启动 Spring 容器，手动构造 {@link JwtConfig}，完全在内存中运行。
 * 覆盖场景：token 生成、userId 解析、有效性校验、过期 token、篡改/错误密钥 token。
 */
@DisplayName("JwtServiceImpl 单元测试")
class JwtServiceImplTest {

    /** 测试用密钥：≥32 字节，满足 HMAC-SHA256 最低要求 */
    private static final String TEST_SECRET =
            "ctrip-test-secret-key-must-be-at-least-32-bytes!!";
    private static final long ACCESS_EXPIRATION_MS = 900_000L; // 15 分钟
    private static final String ISSUER = "ctrip-test";

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret(TEST_SECRET);
        config.setAccessTokenExpirationMs(ACCESS_EXPIRATION_MS);
        config.setRefreshTokenExpirationMs(1_296_000_000L);
        config.setIssuer(ISSUER);
        jwtService = new JwtServiceImpl(config);
    }

    // ── generateAccessToken ───────────────────────────────────────────────────

    @Test
    @DisplayName("generateAccessToken：返回三段式 JWT 字符串")
    void generateAccessToken_shouldReturnValidJwtFormat() {
        String token = jwtService.generateAccessToken(42L, UserRole.USER);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("generateAccessToken：不同 userId 生成不同 token")
    void generateAccessToken_differentUserIds_shouldProduceDifferentTokens() {
        String token1 = jwtService.generateAccessToken(1L, UserRole.USER);
        String token2 = jwtService.generateAccessToken(2L, UserRole.USER);

        assertThat(token1).isNotEqualTo(token2);
    }

    // ── extractUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUserId：从有效 token 正确解析 userId")
    void extractUserId_validToken_shouldReturnCorrectUserId() {
        Long userId = 99L;
        String token = jwtService.generateAccessToken(userId, UserRole.USER);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("extractUserId：过期 token 抛出 TokenExpiredException")
    void extractUserId_expiredToken_shouldThrowTokenExpiredException() {
        // 用同一密钥但 -60 秒过期时间生成已过期 token
        JwtConfig cfg = new JwtConfig();
        cfg.setSecret(TEST_SECRET);
        cfg.setAccessTokenExpirationMs(-60_000L);
        cfg.setIssuer(ISSUER);
        String expiredToken = new JwtServiceImpl(cfg).generateAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> jwtService.extractUserId(expiredToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("过期");
    }

    @Test
    @DisplayName("extractUserId：签名末尾被篡改 → 抛出 AuthenticationException")
    void extractUserId_tamperedToken_shouldThrowAuthenticationException() {
        String token = jwtService.generateAccessToken(1L, UserRole.USER);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractUserId(tampered))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("extractUserId：完全非法字符串 → 抛出 AuthenticationException")
    void extractUserId_malformedString_shouldThrowAuthenticationException() {
        assertThatThrownBy(() -> jwtService.extractUserId("not-a-jwt"))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("extractUserId：错误密钥签名的 token → 抛出 AuthenticationException")
    void extractUserId_wrongSecretToken_shouldThrowAuthenticationException() {
        JwtConfig wrongCfg = new JwtConfig();
        wrongCfg.setSecret("wrong-secret-key-also-must-be-32-bytes-longXXXX");
        wrongCfg.setAccessTokenExpirationMs(ACCESS_EXPIRATION_MS);
        wrongCfg.setIssuer(ISSUER);
        String tokenFromWrongSecret = new JwtServiceImpl(wrongCfg).generateAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> jwtService.extractUserId(tokenFromWrongSecret))
                .isInstanceOf(AuthenticationException.class);
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid：有效 token → true")
    void isTokenValid_validToken_shouldReturnTrue() {
        assertThat(jwtService.isTokenValid(jwtService.generateAccessToken(1L, UserRole.USER))).isTrue();
    }

    @Test
    @DisplayName("isTokenValid：过期 token → false（不抛异常）")
    void isTokenValid_expiredToken_shouldReturnFalse() {
        JwtConfig cfg = new JwtConfig();
        cfg.setSecret(TEST_SECRET);
        cfg.setAccessTokenExpirationMs(-60_000L);
        cfg.setIssuer(ISSUER);
        String expiredToken = new JwtServiceImpl(cfg).generateAccessToken(1L, UserRole.USER);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid：无效字符串 → false（不抛异常）")
    void isTokenValid_invalidString_shouldReturnFalseWithoutException() {
        assertThat(jwtService.isTokenValid("garbage")).isFalse();
    }
}
