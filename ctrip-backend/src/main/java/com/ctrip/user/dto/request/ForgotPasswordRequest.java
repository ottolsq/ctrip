package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 忘记密码请求 DTO。
 *
 * <p>系统将向 {@code credential} 对应的邮箱或手机发送一次性验证码（OTP）。
 * 无论账号是否存在，接口均返回成功（防止用户枚举攻击）。
 */
public record ForgotPasswordRequest(

        /** 注册时使用的邮箱或手机号 */
        @NotBlank
        String credential

) {}
