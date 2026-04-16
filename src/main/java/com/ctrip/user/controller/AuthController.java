package com.ctrip.user.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.user.dto.request.*;
import com.ctrip.user.dto.response.MessageResponse;
import com.ctrip.user.dto.response.TokenResponse;
import com.ctrip.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器，处理注册、登录、令牌刷新、登出及密码重置等无需 JWT 鉴权的端点。
 *
 * <p>所有端点均映射在 /api/v1/auth/** 下，已在 SecurityConfig 中配置为白名单。
 * Controller 层职责：解析请求体、触发 Bean Validation、调用 Service、包装 ApiResponse。
 * 业务逻辑不在此层。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 手机号 + 短信验证码注册。
     * 成功后直接颁发令牌对（手机注册立即激活账号）。
     */
    @PostMapping("/register/phone")
    public ResponseEntity<ApiResponse<TokenResponse>> registerByPhone(
            @Valid @RequestBody RegisterByPhoneRequest request) {
        TokenResponse token = authService.registerByPhone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(token));
    }

    /**
     * 邮箱 + 密码注册。
     * 注册后账号状态为 UNVERIFIED，仍可登录（邮件验证功能待后续实现）。
     */
    @PostMapping("/register/email")
    public ResponseEntity<ApiResponse<TokenResponse>> registerByEmail(
            @Valid @RequestBody RegisterByEmailRequest request) {
        TokenResponse token = authService.registerByEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(token));
    }

    /**
     * 登录（手机号或邮箱 + 密码）。
     * credential 字段含 "@" 时按邮箱查找，否则按手机号查找。
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    /**
     * 使用 refresh token 刷新 access token（令牌轮换策略：旧 token 吊销，颁发新令牌对）。
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse token = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    /**
     * 登出：吊销指定 refresh token（幂等操作）。
     * 客户端应同时丢弃本地保存的 access token。
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("登出成功")));
    }

    /**
     * 发起密码重置：向 credential 关联的邮箱或手机发送 OTP。
     * 防用户枚举：无论用户是否存在均返回相同响应。
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("若账号存在，验证码已发送")));
    }

    /**
     * 完成密码重置：提交 OTP 和新密码，验证通过后更新密码并吊销所有 refresh token。
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("密码重置成功，请重新登录")));
    }

    /**
     * 发送手机短信验证码（注册前调用）。
     * 开发阶段：验证码通过 log.info 输出，不真正发送短信。
     */
    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendSmsCode(
            @Valid @RequestBody SendSmsRequest request) {
        authService.sendSmsCode(request.phone());
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("验证码已发送")));
    }
}
