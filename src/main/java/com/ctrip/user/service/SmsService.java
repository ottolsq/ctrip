package com.ctrip.user.service;

/**
 * 短信服务接口。
 *
 * <p>提供验证码发送与校验能力：
 * <ul>
 *   <li>注册阶段：由实现类自行生成随机码并存储，{@link #sendVerificationCode} 发送；
 *       {@link #validateVerificationCode} 校验后消费该码。</li>
 *   <li>密码重置阶段：业务层自行生成 OTP，通过 {@link #sendOtp} 投递指定内容。</li>
 * </ul>
 */
public interface SmsService {

    /**
     * 向指定手机号发送随机验证码（由实现类内部生成并缓存）。
     *
     * @param phone 目标手机号（E.164 格式，如 +8613800138000）
     */
    void sendVerificationCode(String phone);

    /**
     * 校验手机验证码。
     *
     * <p>校验通过后，该验证码立即失效（一次性使用）。
     * 验证码不存在、已过期或值不匹配时抛出 {@link com.ctrip.common.exception.BusinessException}。
     *
     * @param phone 手机号
     * @param code  用户提交的验证码
     */
    void validateVerificationCode(String phone, String code);

    /**
     * 向指定手机号发送指定内容的 OTP（用于忘记密码流程）。
     *
     * @param phone 目标手机号
     * @param otp   由业务层生成的一次性密码字符串
     */
    void sendOtp(String phone, String otp);
}
