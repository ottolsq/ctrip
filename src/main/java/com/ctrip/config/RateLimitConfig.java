package com.ctrip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流配置属性类，绑定 application.properties 中 app.rate-limit.auth.* 的配置项。
 *
 * <p>配置示例：
 * <pre>
 * app.rate-limit.auth.capacity=10
 * app.rate-limit.auth.refill-tokens=10
 * app.rate-limit.auth.refill-duration-seconds=60
 * </pre>
 *
 * <p>各端点实际限制在 RateLimitInterceptor 中基于此配置按倍数缩减：
 * <ul>
 *   <li>POST /auth/login：5次/分钟/IP</li>
 *   <li>POST /auth/register/**：3次/分钟/IP</li>
 *   <li>POST /auth/password/forgot：3次/分钟/IP</li>
 *   <li>POST /auth/sms/send：2次/分钟/IP</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.rate-limit.auth")
public class RateLimitConfig {

    /** 令牌桶容量（最大突发请求数） */
    private int capacity = 10;

    /** 每次补充的令牌数 */
    private int refillTokens = 10;

    /** 补充令牌的时间窗口（秒） */
    private int refillDurationSeconds = 60;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    public int getRefillDurationSeconds() {
        return refillDurationSeconds;
    }

    public void setRefillDurationSeconds(int refillDurationSeconds) {
        this.refillDurationSeconds = refillDurationSeconds;
    }
}
