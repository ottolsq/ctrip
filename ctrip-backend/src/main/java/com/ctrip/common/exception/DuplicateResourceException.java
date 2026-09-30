package com.ctrip.common.exception;

/**
 * 资源重复异常，对应 HTTP 409 Conflict。
 *
 * <p>用于唯一性约束冲突的情况，例如注册时用户名、邮箱或手机号已被占用。
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
