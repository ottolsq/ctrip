package com.ctrip.content.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 布隆过滤器集成测试——使用真实 Redisson RBloomFilter，手动连接 Redis。
 */
@DisplayName("布隆过滤器集成测试")
class BloomFilterTest {

    private static final String TEST_BLOOM_KEY = "test:bloom:dest:ids";
    private static final long EXPECTED_ELEMENTS = 1_000L;
    private static final double FALSE_PROBABILITY = 0.01;

    private static RedissonClient redissonClient;
    private RBloomFilter<String> filter;

    @BeforeAll
    static void initRedisson() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        redissonClient = Redisson.create(config);
    }

    @BeforeEach
    void setUp() {
        filter = redissonClient.getBloomFilter(TEST_BLOOM_KEY);
        filter.delete();
        filter.tryInit(EXPECTED_ELEMENTS, FALSE_PROBABILITY);
    }

    @AfterEach
    void tearDown() {
        filter.delete();
    }

    @Test
    @DisplayName("已知 ID 通过过滤：add 后 contains 返回 true")
    void contains_knownId_shouldReturnTrue() {
        filter.add("dest:1");
        assertThat(filter.contains("dest:1")).isTrue();
    }

    @Test
    @DisplayName("不存在 ID 被拦截：未 add 的 ID contains 返回 false")
    void contains_unknownId_shouldReturnFalse() {
        assertThat(filter.contains("dest:99999")).isFalse();
    }

    @Test
    @DisplayName("运行时新增 ID：add 后立即可被 contains 识别")
    void add_runtimeAddition_shouldBeRecognizedImmediately() {
        assertThat(filter.contains("dest:new_runtime")).isFalse();
        filter.add("dest:new_runtime");
        assertThat(filter.contains("dest:new_runtime")).isTrue();
    }

    @Test
    @DisplayName("批量添加 100 个元素后全部可识别")
    void add_batchAddition_shouldAllBeRecognized() {
        for (long i = 1; i <= 100; i++) {
            filter.add("dest:" + i);
        }
        for (long i = 1; i <= 100; i++) {
            assertThat(filter.contains("dest:" + i))
                    .as("dest:%d should be in bloom filter", i).isTrue();
        }
    }
}
