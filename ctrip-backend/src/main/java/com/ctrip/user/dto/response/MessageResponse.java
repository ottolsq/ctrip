package com.ctrip.user.dto.response;

/**
 * 通用消息响应 DTO，用于无需返回数据的成功操作（如登出、发送验证码、忘记密码等）。
 *
 * @param message 操作结果说明
 */
public record MessageResponse(String message) {}
