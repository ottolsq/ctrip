package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求 DTO。
 *
 * <p>需先调用 {@code /api/v1/auth/password/forgot} 获取 OTP，
 * 再携带 OTP 和新密码调用此接口完成密码重置。
 */
public record ResetPasswordRequest(

        /** 注册时使用的邮箱或手机号 */
        @NotBlank
        String credential,

        /** 一次性验证码（OTP），固定 6 位数字 */
        @NotBlank @Size(min = 6, max = 6)
        String otp,

        /** 新密码，8~64 位 */
        @NotBlank @Size(min = 8, max = 64)
        String newPassword

) {}
