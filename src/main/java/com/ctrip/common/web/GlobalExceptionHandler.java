package com.ctrip.common.web;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.DuplicateResourceException;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.common.response.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，将所有业务异常统一映射为 {@link ApiResponse} 格式的 HTTP 响应。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>错误消息只向客户端暴露对用户可见的描述，从不返回堆栈信息或内部路径。
 *   <li>500 级别的未预期异常必须在服务端以 ERROR 级别记录完整堆栈，方便排查。
 *   <li>每类异常有且仅有一个 @ExceptionHandler，避免多重处理造成歧义。
 * </ul>
 *
 * <h3>HTTP 状态码映射</h3>
 * <pre>
 * ResourceNotFoundException    → 404 Not Found
 * AuthenticationException      → 401 Unauthorized  (凭据无效)
 * TokenExpiredException        → 401 Unauthorized  (token 过期，客户端应尝试刷新)
 * ForbiddenException           → 403 Forbidden     (权限不足)
 * DuplicateResourceException   → 409 Conflict      (唯一性冲突，如邮箱已注册)
 * BusinessException            → 400 Bad Request   (业务规则违反)
 * MethodArgumentNotValidException → 400 Bad Request (Bean Validation 失败)
 * Exception (兜底)              → 500 Internal Server Error
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 资源不存在：404。 */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /**
     * 认证失败：401。
     *
     * <p>注意：消息中不区分"用户不存在"和"密码错误"，防止用户枚举攻击。
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthentication(AuthenticationException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /**
     * Token 过期：401。
     *
     * <p>与 AuthenticationException 分开处理，使客户端可以区分两种 401：
     * <ul>
     *   <li>AuthenticationException → 凭据错误，需要重新登录
     *   <li>TokenExpiredException   → token 过期，可尝试用 refresh token 续期
     * </ul>
     */
    @ExceptionHandler(TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleTokenExpired(TokenExpiredException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** 权限不足：403。 */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbidden(ForbiddenException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** 参数非法：400（日期校验、枚举非法值等）。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** 资源重复（唯一性冲突）：409。 */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateResource(DuplicateResourceException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** 业务规则违反：400。 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /**
     * Bean Validation 失败（@Valid / @Validated）：400。
     *
     * <p>将所有字段错误收集为 "field: message; ..." 格式返回，
     * 方便前端直接展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(message);
    }

    /**
     * 兜底异常处理：500。
     *
     * <p>所有未被上方处理器覆盖的异常都在此捕获。
     * 服务端记录完整堆栈，客户端只收到通用错误消息，防止内部信息泄漏。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        log.error("未预期的异常", ex);
        return ApiResponse.error("服务器内部错误，请稍后重试");
    }
}
