package com.ctrip.blindbox.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秒杀 Lua 脚本集成测试——使用真实 Redis 执行 {@code seckill.lua}，手动连接。
 *
 * <p>通过 StringRedisTemplate 直接执行 Lua 脚本，验证返回值 + Redis 副作用。
 * 覆盖：正常秒杀、库存刚好用完、库存耗尽、未预热、重复购买、并发多用户。
 */
@DisplayName("秒杀 Lua 脚本集成测试")
class FlashSaleServiceTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final String STOCK_KEY = "seckill:stock:1";
    private static final String USERS_KEY = "seckill:users:1";

    private static StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<Long> seckillScript;

    @BeforeAll
    static void initRedis() {
        // 手动连接 Redis（localhost:6379，与 application.properties 配置一致）
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        stringRedisTemplate = new StringRedisTemplate(factory);
        stringRedisTemplate.afterPropertiesSet();
    }

    @BeforeEach
    void setUp() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);
    }

    @AfterEach
    void cleanUp() {
        stringRedisTemplate.delete(List.of(STOCK_KEY, USERS_KEY));
    }

    /** 执行 Lua 脚本并返回结果 */
    private Long execute(Long userId) {
        return stringRedisTemplate.execute(
                seckillScript, List.of(TEMPLATE_ID.toString()), userId.toString());
    }

    private void setStock(int stock) {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock));
    }

    private void clearUsers() {
        stringRedisTemplate.delete(USERS_KEY);
    }

    @Nested
    @DisplayName("基本秒杀场景")
    class BasicScenarios {

        @Test
        @DisplayName("正常秒杀成功：stock=10 → 9，用户加入集合")
        void execute_stockAvailable_shouldReturnSuccessAndDecrementStock() {
            setStock(10);
            clearUsers();

            Long result = execute(100L);

            assertThat(result).isEqualTo(1L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("9");
            assertThat(stringRedisTemplate.opsForSet().isMember(USERS_KEY, "100")).isTrue();
        }

        @Test
        @DisplayName("库存刚好用完：stock=1 → 0")
        void execute_lastStock_shouldSucceedAndReduceToZero() {
            setStock(1);
            clearUsers();

            Long result = execute(200L);

            assertThat(result).isEqualTo(1L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("0");
            assertThat(stringRedisTemplate.opsForSet().isMember(USERS_KEY, "200")).isTrue();
        }

        @Test
        @DisplayName("库存已耗尽：stock=0 → 返回 0，库存和用户集合均不变")
        void execute_stockExhausted_shouldReturnSoldOut() {
            setStock(0);
            clearUsers();

            Long result = execute(300L);

            assertThat(result).isEqualTo(0L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("0");
            assertThat(stringRedisTemplate.opsForSet().isMember(USERS_KEY, "300")).isFalse();
        }
    }

    @Nested
    @DisplayName("库存 Key 不存在（未预热）")
    class NoPrewarm {

        @Test
        @DisplayName("库存 Key 不存在时返回 0，不会创建空的 stock key")
        void execute_noStockKey_shouldReturnSoldOutAndNotCreateDirtyKey() {
            stringRedisTemplate.delete(STOCK_KEY);
            clearUsers();

            Long result = execute(400L);

            assertThat(result).isEqualTo(0L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isNull();
            assertThat(stringRedisTemplate.opsForSet().isMember(USERS_KEY, "400")).isFalse();
        }
    }

    @Nested
    @DisplayName("重复购买防护")
    class DuplicatePrevention {

        @Test
        @DisplayName("重复购买：同一用户再次秒杀 → 返回 -1，库存不扣减")
        void execute_duplicateUser_shouldReturnDuplicateAndNotDecrementStock() {
            setStock(5);
            clearUsers();
            Long first = execute(500L);
            assertThat(first).isEqualTo(1L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("4");

            Long second = execute(500L);

            assertThat(second).isEqualTo(-1L);
            // 库存未再次扣减（防超卖核心！）
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("4");
        }
    }

    @Nested
    @DisplayName("并发多用户（顺序模拟）")
    class MultiUser {

        @Test
        @DisplayName("5 个不同用户竞抢 5 件库存 → 全部成功，stock=0")
        void execute_fiveUsersWithFiveStock_shouldAllSucceed() {
            setStock(5);
            clearUsers();

            Long[] results = new Long[5];
            for (int i = 0; i < 5; i++) {
                results[i] = execute((long) (1000 + i));
            }

            assertThat(results).containsExactly(1L, 1L, 1L, 1L, 1L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("0");
            assertThat(stringRedisTemplate.opsForSet().size(USERS_KEY)).isEqualTo(5L);
            for (int i = 0; i < 5; i++) {
                assertThat(stringRedisTemplate.opsForSet()
                        .isMember(USERS_KEY, String.valueOf(1000 + i))).isTrue();
            }
        }

        @Test
        @DisplayName("第 6 个用户在第 5 个成功后秒杀 → 库存不足")
        void execute_sixthUserAfterFive_shouldGetSoldOut() {
            setStock(5);
            clearUsers();
            for (int i = 0; i < 5; i++) {
                assertThat(execute((long) (2000 + i))).isEqualTo(1L);
            }

            Long sixth = execute(2005L);

            assertThat(sixth).isEqualTo(0L);
            assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("0");
            assertThat(stringRedisTemplate.opsForSet().isMember(USERS_KEY, "2005")).isFalse();
        }
    }
}
