package com.ctrip.user.service;

import com.ctrip.user.dto.request.*;
import com.ctrip.user.dto.response.TokenResponse;

/**
 * 认证服务接口，负责用户注册、登录、令牌刷新与登出、密码重置等认证生命周期管理。
 */
public interface AuthService {

    /**
     * 通过手机号注册（需先通过短信验证码校验）。
     *
     * @param request 注册请求（手机号、短信验证码、密码、用户名）
     * @return 包含 accessToken 和 refreshToken 的令牌响应
     */
    TokenResponse registerByPhone(RegisterByPhoneRequest request);

    /**
     * 通过邮箱注册（账号初始状态为 UNVERIFIED，邮件验证后续实现）。
     *
     * @param request 注册请求（邮箱、密码、用户名）
     * @return 包含 accessToken 和 refreshToken 的令牌响应
     */
    TokenResponse registerByEmail(RegisterByEmailRequest request);

    /**
     * 用户登录（credential 支持邮箱或手机号）。
     *
     * @param request 登录请求（credential + 密码）
     * @return 包含 accessToken 和 refreshToken 的令牌响应
     */
    TokenResponse login(LoginRequest request);

    /**
     * 使用 refresh token 换取新的令牌对（token 轮换策略）。
     *
     * @param request 包含原始 refreshToken 的请求
     * @return 新的令牌响应
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * 登出：吊销指定的 refresh token。
     *
     * @param rawRefreshToken 客户端持有的原始 refreshToken 值
     */
    void logout(String rawRefreshToken);

    /**
     * 忘记密码：向用户绑定的手机或邮箱发送 OTP。
     *
     * <p>用户不存在时静默返回（防止用户枚举攻击）。
     *
     * @param request 包含 credential（邮箱或手机号）的请求
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * 重置密码：使用 OTP 设置新密码，并吊销该用户所有 refresh token。
     *
     * @param request 包含 credential、OTP 和新密码的请求
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 发送短信验证码（注册前调用）。
     * 开发阶段：验证码通过 log.debug 输出，不真正发送短信。
     *
     * @param phone 目标手机号
     */
    void sendSmsCode(String phone);
}
