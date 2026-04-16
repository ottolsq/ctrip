package com.ctrip.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * EmailService 开发阶段桩实现。
 *
 * <p>不真正发送邮件，仅通过 {@code log.debug} 输出 OTP 内容，方便开发调试。
 * 生产环境须在 prod profile 下注册真实邮件发送实现（如 JavaMail / SendGrid）以替换此 Bean。
 */
@Profile("!prod")
@Service
public class StubEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(StubEmailServiceImpl.class);

    /**
     * {@inheritDoc}
     *
     * <p>仅打印日志，不发送真实邮件。
     */
    @Override
    public void sendPasswordResetCode(String email, String otp) {
        log.debug("[StubEmail] 密码重置 OTP 已发送 email={} otp={}", email, otp);
    }
}
