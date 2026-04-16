package com.ctrip;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 上下文加载集成测试。
 *
 * <p>使用 H2 内存数据库替换 MySQL（{@code Replace.ANY}），
 * 注入测试用 JWT secret（满足 ≥ 32 字节要求），
 * 保证 CI 环境无需真实数据库连接即可通过上下文启动验证。
 */
@SpringBootTest(properties = {
    "app.jwt.secret=test-secret-key-minimum-32-bytes!!"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CtripApplicationTests {

    @Test
    void contextLoads() {
    }

}
