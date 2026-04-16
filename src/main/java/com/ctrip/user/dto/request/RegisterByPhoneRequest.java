package com.ctrip.user.dto.request;

import com.ctrip.common.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 手机号注册请求 DTO。
 *
 * <p>注册流程：先调用 {@code /api/v1/auth/sms/send} 获取短信验证码，
 * 再携带验证码调用此接口完成注册。
 */
public record RegisterByPhoneRequest(

        /** 手机号，格式：可选 + 前缀，8~15 位数字 */
        @NotBlank @PhoneNumber
        String phone,

        /** 短信验证码，固定 6 位数字 */
        @NotBlank @Size(min = 6, max = 6)
        String smsCode,

        /** 登录密码，8~64 位 */
        @NotBlank @Size(min = 8, max = 64)
        String password,

        /** 用户名，2~50 位 */
        @NotBlank @Size(min = 2, max = 50)
        String username

) {}
