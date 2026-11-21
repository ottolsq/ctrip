package com.ctrip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MVC ObjectMapper 日期序列化契约：输出 ISO 8601 字符串，而非时间戳数组。
 */
@SpringBootTest(properties = {
    "app.jwt.secret=test-secret-key-minimum-32-bytes!!",
    "app.cache.bloom.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ObjectMapperConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("LocalDateTime 序列化为 ISO 8601 字符串")
    void localDateTimeShouldSerializeAsIsoString() throws Exception {
        String json = objectMapper.writeValueAsString(
                Map.of("createdAt", LocalDateTime.of(2026, 11, 21, 10, 2, 3)));

        assertThat(json).isEqualTo("{\"createdAt\":\"2026-11-21T10:02:03\"}");
    }
}
