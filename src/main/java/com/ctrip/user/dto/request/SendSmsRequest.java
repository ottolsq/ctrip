package com.ctrip.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送短信验证码请求 DTO。
 *
 * @param phone 接收验证码的手机号（国际格式，7-15 位数字，可含 "+" 前缀）
 */
public record SendSmsRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "手机号格式不正确")
        String phone
) {}
