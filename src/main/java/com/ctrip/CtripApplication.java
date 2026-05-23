package com.ctrip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口。
 *
 * <p>{@code @MapperScan} 显式指定 mapper 包路径，确保在 Spring Boot 4.x 下
 * MyBatis Plus 的自动配置未完全激活时 Mapper 接口仍能正确注册为 Spring Bean。
 */
@SpringBootApplication
@MapperScan({"com.ctrip.user.mapper", "com.ctrip.content.mapper"})
public class CtripApplication {

	public static void main(String[] args) {
		SpringApplication.run(CtripApplication.class, args);
	}

}
