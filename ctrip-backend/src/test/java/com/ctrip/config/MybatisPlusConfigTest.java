package com.ctrip.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MybatisPlusConfig} 单元测试。
 */
@DisplayName("MyBatis Plus 配置单元测试")
class MybatisPlusConfigTest {

    @Test
    @DisplayName("分页拦截器已注册")
    void mybatisPlusInterceptor_shouldContainPaginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
                .anyMatch(inner -> inner instanceof PaginationInnerInterceptor);
    }
}
