package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO。
 *
 * <p>{@code credential} 可以是邮箱地址或手机号，服务层通过是否包含 {@code @} 字符自动识别。
 */
public record LoginRequest(

        /** 登录凭据：邮箱地址或手机号 */
        @NotBlank
        String credential,

        /** 登录密码 */
        @NotBlank
        String password

) {}
