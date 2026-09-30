package com.ctrip.ai.exception;

/**
 * AI 服务通用异常。
 *
 * <p>AI 调用失败时抛出，业务模块需自行处理（如 fallback 到 mock 数据）。
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
