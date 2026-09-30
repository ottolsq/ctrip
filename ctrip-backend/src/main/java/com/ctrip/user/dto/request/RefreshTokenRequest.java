package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新 Access Token 请求 DTO。
 *
 * <p>{@code refreshToken} 为原始值（非哈希），服务端会计算 SHA-256 后查库校验。
 */
public record RefreshTokenRequest(

        /** Refresh token 原始值（登录/注册响应中返回的 refreshToken 字段） */
        @NotBlank
        String refreshToken

) {}
