package com.ctrip.config;

import com.ctrip.user.security.RateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类，负责注册限流拦截器并激活限流配置属性绑定。
 *
 * <p>{@code @EnableConfigurationProperties(RateLimitConfig.class)} 使
 * {@link RateLimitConfig} 作为 Spring Bean 可被注入，
 * 即使 {@link RateLimitConfig} 本身未标注 {@code @Component}。
 */
@Configuration
@EnableConfigurationProperties(RateLimitConfig.class)
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * 将限流拦截器注册到所有认证端点路径。
     * 仅对 /api/v1/auth/** 生效，已认证的用户端点（/api/v1/users/**）不限流。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/**");
    }
}
