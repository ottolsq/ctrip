package com.ctrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

/**
 * Redis 连接配置。
 *
 * <p>Spring Boot 自动装配 Redis 连接（读取 spring.data.redis.* 配置），
 * 此处提供 StringRedisTemplate Bean 供业务使用。
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate — 用于 KV 字符串操作（分布式锁、缓存、计数器等）。
     *
     * <p>使用 GenericToStringSerializer 保证 key/value 以纯字符串形式存储，
     * 便于调试和与 Redis CLI 交互。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.setHashKeySerializer(new GenericToStringSerializer<>(String.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(String.class));
        return template;
    }
}
