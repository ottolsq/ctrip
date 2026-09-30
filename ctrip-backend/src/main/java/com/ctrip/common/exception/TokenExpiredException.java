package com.ctrip.common.exception;

/**
 * Token 过期异常，对应 HTTP 401 Unauthorized。
 *
 * <p>与 AuthenticationException 分离，使客户端可以区分"凭据错误"和"token 过期"，
 * 从而决定是重新登录还是直接用 refresh token 续期。
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
