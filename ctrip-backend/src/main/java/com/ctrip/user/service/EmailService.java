package com.ctrip.user.service;

/**
 * 邮件服务接口。
 *
 * <p>目前仅支持发送密码重置 OTP 邮件。
 * 开发阶段由桩实现（{@link StubEmailServiceImpl}）通过日志输出代替真实投递。
 */
public interface EmailService {

    /**
     * 向指定邮箱发送密码重置 OTP。
     *
     * @param email 目标邮箱地址
     * @param otp   由业务层生成的一次性密码字符串
     */
    void sendPasswordResetCode(String email, String otp);
}
