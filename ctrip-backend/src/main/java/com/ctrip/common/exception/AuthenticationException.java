package com.ctrip.common.exception;

/**
 * 认证失败异常，对应 HTTP 401 Unauthorized。
 *
 * <p>用于凭据无效的情况，例如密码错误、账号被封禁等。
 * 注意：不要在消息中区分"用户不存在"和"密码错误"，避免用户枚举攻击。
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
