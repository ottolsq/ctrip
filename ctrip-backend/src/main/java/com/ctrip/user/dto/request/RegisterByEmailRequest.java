package com.ctrip.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 邮箱注册请求 DTO。
 *
 * <p>注册后账号状态为 {@code UNVERIFIED}，邮件验证流程待后续版本实现。
 * 当前版本 UNVERIFIED 用户可正常使用系统功能。
 */
public record RegisterByEmailRequest(

        /** 邮箱地址 */
        @NotBlank @Email
        String email,

        /** 登录密码，8~64 位 */
        @NotBlank @Size(min = 8, max = 64)
        String password,

        /** 用户名，2~50 位 */
        @NotBlank @Size(min = 2, max = 50)
        String username

) {}
