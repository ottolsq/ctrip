package com.ctrip.user.dto.response;

/**
 * 认证令牌响应 DTO，登录和注册成功后返回。
 *
 * @param accessToken  JWT access token（有效期 15 分钟）
 * @param refreshToken Refresh token 原始值（有效期 15 天）；服务端只存其 SHA-256 哈希，不存原始值
 * @param expiresIn    Access token 有效期（秒）
 * @param tokenType    令牌类型，固定为 "Bearer"
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {}
