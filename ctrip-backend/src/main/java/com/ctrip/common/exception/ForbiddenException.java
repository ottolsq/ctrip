package com.ctrip.common.exception;

/**
 * 权限不足 — 已认证用户尝试操作不属于自己的资源。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
