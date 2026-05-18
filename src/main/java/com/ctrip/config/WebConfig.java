package com.ctrip.config;

import com.ctrip.user.security.RateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/**");
    }

    /**
     * 注册静态资源映射，使上传的图片可通过 URL 访问。
     *
     * <p>外部请求 {@code /uploads/images/**} 映射到本地目录 {@code uploads/images/}。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
