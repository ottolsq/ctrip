package com.ctrip.content.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheService} 缓存读写集成测试——使用真实 Redis，手动连接，不依赖 Spring 容器。
 *
 * <p>测试覆盖：缓存命中/未命中、空值穿透防护、TTL 雪崩防护（随机偏移）、
 * 缓存反序列化失败兜底、单 key 删除、模式批量删除。
 */
@DisplayName("CacheService 缓存读写集成测试")
class CacheServiceTest {

    private static final String TEST_KEY = "test:cache:single";
    private static final Duration TEST_TTL = Duration.ofMinutes(5);

    private static StringRedisTemplate stringRedisTemplate;
    private static CacheService cacheService;

    @BeforeAll
    static void setUp() {
        // 手动连接 Redis（localhost:6379，与 application.properties 配置一致）
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        stringRedisTemplate = new StringRedisTemplate(factory);
        stringRedisTemplate.afterPropertiesSet();
        cacheService = new CacheService(stringRedisTemplate, new ObjectMapper());
    }

    @AfterEach
    void cleanUp() {
        stringRedisTemplate.delete("content:" + TEST_KEY);
        Set<String> patternKeys = stringRedisTemplate.keys("content:test:*");
        if (patternKeys != null && !patternKeys.isEmpty()) {
            stringRedisTemplate.delete(patternKeys);
        }
    }

    @Nested
    @DisplayName("缓存读写（Cache-Aside 模式）")
    class ReadWrite {

        @Test
        @DisplayName("缓存命中：Redis 中有有效 JSON → 直接返回，不触发 dbLoader")
        void getOrLoad_cacheHit_shouldReturnCachedData() {
            stringRedisTemplate.opsForValue().set("content:" + TEST_KEY, "\"北京\"");
            AtomicBoolean dbLoaderCalled = new AtomicBoolean(false);

            String result = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> { dbLoaderCalled.set(true); return "from-db"; }, TEST_TTL);

            assertThat(result).isEqualTo("北京");
            assertThat(dbLoaderCalled.get()).isFalse();
        }

        @Test
        @DisplayName("缓存未命中→查DB→回写：dbLoader 被调用，结果写入 Redis")
        void getOrLoad_cacheMiss_shouldLoadFromDbAndWriteBack() {
            stringRedisTemplate.delete("content:" + TEST_KEY);

            String result = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> "from-database", TEST_TTL);

            assertThat(result).isEqualTo("from-database");
            String cached = stringRedisTemplate.opsForValue().get("content:" + TEST_KEY);
            assertThat(cached).isEqualTo("\"from-database\"");
        }

        @Test
        @DisplayName("空值缓存命中：DB 返回 null → 缓存空值标记 → 下次直接返回 null")
        void getOrLoad_nullValueCached_shouldReturnNullWithoutCallingDbLoader() {
            stringRedisTemplate.delete("content:" + TEST_KEY);
            String result1 = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> null, TEST_TTL);
            assertThat(result1).isNull();

            String cached = stringRedisTemplate.opsForValue().get("content:" + TEST_KEY);
            assertThat(cached).isEqualTo("{\"__null__\":true}");

            AtomicBoolean dbLoaderCalled = new AtomicBoolean(false);
            String result2 = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> { dbLoaderCalled.set(true); return "should-not-reach"; }, TEST_TTL);

            assertThat(result2).isNull();
            assertThat(dbLoaderCalled.get()).isFalse();
        }

        @Test
        @DisplayName("空值缓存被覆盖：手动写入真实数据后，可正常读取")
        void getOrLoad_nullValueOverwritten_shouldReadRealData() {
            stringRedisTemplate.opsForValue().set(
                    "content:" + TEST_KEY, "{\"__null__\":true}", Duration.ofMinutes(1));
            stringRedisTemplate.opsForValue().set("content:" + TEST_KEY, "\"已修复\"");

            String result = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> "fallback", TEST_TTL);

            assertThat(result).isEqualTo("已修复");
        }

        @Test
        @DisplayName("缓存反序列化失败：Redis 中为非法 JSON → 删除坏缓存 → 走 DB 加载")
        void getOrLoad_brokenJson_shouldDeleteCacheAndLoadFromDb() {
            stringRedisTemplate.opsForValue().set("content:" + TEST_KEY, "{broken-json!!!");

            String result = cacheService.getOrLoad(TEST_KEY, String.class,
                    () -> "recovered-from-db", TEST_TTL);

            assertThat(result).isEqualTo("recovered-from-db");
            String afterCleanup = stringRedisTemplate.opsForValue().get("content:" + TEST_KEY);
            assertThat(afterCleanup).isNotNull();
            assertThat(afterCleanup).isNotEqualTo("{broken-json!!!");
        }
    }

    @Nested
    @DisplayName("TTL 雪崩防护（随机偏移 ±10%）")
    class TtlRandomization {

        @Test
        @DisplayName("TTL 在基准值的 90%~110% 范围内随机分布")
        void getOrLoad_ttlShouldBeWithin90To110PercentOfBaseTtl() {
            Duration baseTtl = Duration.ofSeconds(100);
            long minExpected = 90;
            long maxExpected = 110;

            for (int i = 0; i < 10; i++) {
                final int idx = i;
                String key = "test:ttl:" + idx;
                stringRedisTemplate.delete("content:" + key);

                cacheService.getOrLoad(key, String.class,
                        () -> "value-" + idx, baseTtl);

                Long actualSeconds = stringRedisTemplate.getExpire("content:" + key);
                assertThat(actualSeconds)
                        .as("TTL 第 %d 次应在 [%d, %d] 之间，实际=%d",
                                idx, minExpected, maxExpected, actualSeconds)
                        .isGreaterThanOrEqualTo(minExpected)
                        .isLessThanOrEqualTo(maxExpected);

                stringRedisTemplate.delete("content:" + key);
            }
        }
    }

    @Nested
    @DisplayName("缓存失效（写操作后删除）")
    class Eviction {

        @Test
        @DisplayName("evict：删除单个缓存 key")
        void evict_shouldDeleteSingleKey() {
            stringRedisTemplate.opsForValue().set("content:" + TEST_KEY, "some-data");
            cacheService.evict(TEST_KEY);

            String result = stringRedisTemplate.opsForValue().get("content:" + TEST_KEY);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("evictByPattern：批量删除匹配 key，不匹配的保留")
        void evictByPattern_shouldDeleteAllMatchingKeys() {
            for (int i = 1; i <= 3; i++) {
                stringRedisTemplate.opsForValue().set("content:test:list:" + i, "data-" + i);
            }
            stringRedisTemplate.opsForValue().set("content:test:other", "keep-me");

            cacheService.evictByPattern("test:list:*");

            for (int i = 1; i <= 3; i++) {
                assertThat(stringRedisTemplate.opsForValue().get("content:test:list:" + i))
                        .as("key test:list:%d 应被删除", i).isNull();
            }
            assertThat(stringRedisTemplate.opsForValue().get("content:test:other"))
                    .isEqualTo("keep-me");
        }

        @Test
        @DisplayName("evictByPattern：无匹配 key 时静默成功")
        void evictByPattern_noMatchingKeys_shouldSucceedSilently() {
            cacheService.evictByPattern("test:nonexistent:*");
        }
    }
}
