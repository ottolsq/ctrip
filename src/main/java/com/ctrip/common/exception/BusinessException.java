package com.ctrip.common.exception;

/**
 * 业务逻辑异常，对应 HTTP 400 Bad Request。
 *
 * <p>用于请求合法但不满足业务规则的情况，例如密码错误、验证码过期、账号状态异常等。
 * 错误信息会直接返回给客户端，应使用用户可读的描述，不含内部实现细节。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
