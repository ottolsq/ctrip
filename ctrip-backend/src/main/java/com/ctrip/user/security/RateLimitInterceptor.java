package com.ctrip.user.security;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证端点限流拦截器（基于 Bucket4j 令牌桶算法）。
 *
 * <p>按客户端 IP + 请求路径为 key，在内存中维护独立的令牌桶，
 * 各端点限制如下（每 IP 每分钟）：
 * <ul>
 *   <li>POST /api/v1/auth/login：5 次</li>
 *   <li>POST /api/v1/auth/register/**：3 次</li>
 *   <li>POST /api/v1/auth/password/forgot：3 次</li>
 *   <li>POST /api/v1/auth/sms/send：2 次</li>
 *   <li>其他 /api/v1/auth/** 路径：使用配置的默认容量</li>
 * </ul>
 *
 * <p>超出限制时返回 HTTP 429 Too Many Requests 和 JSON 错误体。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 内存令牌桶缓存，key = "clientIp:requestPath" */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String key = clientIp + ":" + path;

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(path));

        if (bucket.tryConsume(1)) {
            return true;
        }

        // 超出限制：返回 429 JSON 响应
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("请求过于频繁，请稍后再试")));
        return false;
    }

    /**
     * 根据请求路径确定限流容量，按设计文档中各端点的限制值创建令牌桶。
     *
     * @param path 请求路径
     * @return 对应的 Bucket4j 令牌桶
     */
    private Bucket createBucket(String path) {
        int capacity = resolveCapacity(path);
        Duration refillDuration = Duration.ofSeconds(rateLimitConfig.getRefillDurationSeconds());

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * 按端点路径返回对应的令牌桶容量（每时间窗口最大请求次数）。
     */
    private int resolveCapacity(String path) {
        if (path.contains("/auth/login")) {
            return 5;
        }
        if (path.contains("/auth/register")) {
            return 3;
        }
        if (path.contains("/auth/password/forgot")) {
            return 3;
        }
        if (path.contains("/auth/sms/send")) {
            return 2;
        }
        // 其他认证端点使用配置的默认值
        return rateLimitConfig.getCapacity();
    }

    /**
     * 解析客户端 IP。
     *
     * <p>开发阶段直接使用 remoteAddr，避免信任可伪造的 X-Forwarded-For 请求头（C-4）。
     * 生产部署时，通过 {@code server.forward-headers-strategy=NATIVE} 让容器/代理
     * 在网络层完成 IP 还原，Spring 将把真实 IP 写入 remoteAddr，此处无需改动。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 字符串
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
