package com.ctrip.common.exception;

/**
 * 资源不存在异常，对应 HTTP 404 Not Found。
 *
 * <p>用于查询时找不到目标实体的情况，例如用户 ID 不存在。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
