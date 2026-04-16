package com.ctrip.user.service;

import com.ctrip.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmsService 开发阶段桩实现。
 *
 * <p>验证码存储在内存 {@link ConcurrentHashMap} 中（有效期 10 分钟），
 * 不依赖外部短信平台。验证码通过 {@code log.debug} 打印，开发环境可直接查看日志。
 *
 * <p>注意：此实现仅在非生产环境（{@code !prod} profile）激活，
 * 生产环境须在 prod profile 下注册真实短信服务实现以替换此 Bean。
 */
@Profile("!prod")
@Service
public class StubSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(StubSmsServiceImpl.class);

    /** 验证码有效期（分钟） */
    private static final int CODE_TTL_MINUTES = 10;

    /** 验证码长度（位数） */
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    /**
     * 内存存储：key = 手机号，value = 验证码条目。
     * 线程安全，支持并发注册请求。
     */
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>生成 6 位随机数字验证码，存入内存缓存，并通过日志输出（开发可见）。
     */
    @Override
    public void sendVerificationCode(String phone) {
        String code = generateCode();
        store.put(phone, new CodeEntry(code, LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES)));
        log.debug("[StubSms] 验证码已发送 phone={} code={}", phone, code);
    }

    /**
     * {@inheritDoc}
     *
     * <p>校验通过后立即从缓存中移除该条目（一次性使用）。
     *
     * @throws BusinessException 验证码不存在、已过期或值不匹配时抛出
     */
    @Override
    public void validateVerificationCode(String phone, String code) {
        CodeEntry entry = store.get(phone);
        if (entry == null || LocalDateTime.now().isAfter(entry.expireAt())) {
            store.remove(phone);
            throw new BusinessException("验证码无效或已过期");
        }
        if (!entry.code().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 校验成功，立即消费
        store.remove(phone);
    }

    /**
     * {@inheritDoc}
     *
     * <p>仅通过日志输出 OTP，不真正发送短信（开发阶段使用）。
     */
    @Override
    public void sendOtp(String phone, String otp) {
        log.debug("[StubSms] OTP 已发送 phone={} otp={}", phone, otp);
    }

    // -----------------------------------------------------------------------
    // 私有辅助方法
    // -----------------------------------------------------------------------

    /** 生成指定位数的随机数字验证码，补零确保固定长度。 */
    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", random.nextInt(bound));
    }

    /**
     * 验证码存储条目（不可变值对象）。
     *
     * @param code     验证码字符串
     * @param expireAt 过期时间
     */
    private record CodeEntry(String code, LocalDateTime expireAt) {}
}
