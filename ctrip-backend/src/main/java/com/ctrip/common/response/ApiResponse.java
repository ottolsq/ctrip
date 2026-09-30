package com.ctrip.common.response;

/**
 * 统一 API 响应信封。
 *
 * <p>所有 Controller 方法返回此类型，格式固定为：
 * <pre>
 * 成功：{ "success": true,  "data": {...}, "error": null    }
 * 失败：{ "success": false, "data": null,  "error": "原因" }
 * </pre>
 *
 * <p>使用示例：
 * <pre>{@code
 * return ResponseEntity.ok(ApiResponse.ok(userProfile));
 * return ResponseEntity.badRequest().body(ApiResponse.error("用户名已存在"));
 * }</pre>
 */
public record ApiResponse<T>(boolean success, T data, String error) {

    /** 构造成功响应，data 为返回的业务数据。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 构造失败响应，message 为面向客户端的错误说明（不含堆栈或内部细节）。 */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
