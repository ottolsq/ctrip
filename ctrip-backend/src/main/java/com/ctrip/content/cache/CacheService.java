package com.ctrip.content.cache;

import com.ctrip.config.CacheConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 通用缓存服务。
 *
 * <p>封装 Cache-Aside 读取模式 + 防穿透（空值缓存）+ 防雪崩（TTL 随机偏移）。
 * 所有内容模块的缓存读写操作均通过此类完成，避免各 Service 各自实现缓存逻辑。
 *
 * <h3>核心方法</h3>
 * <ul>
 *   <li>{@link #getOrLoad(String, Class, Supplier, Duration)} — 读缓存，未命中则查 DB 并回写</li>
 *   <li>{@link #getOrLoad(String, JavaType, Supplier, Duration)} — 同上，支持泛型类型（如 Page）</li>
 *   <li>{@link #evict(String)} — 删除单个缓存 key</li>
 *   <li>{@link #evictByPattern(String)} — 按模式批量删除（如 "dest:list:*"）</li>
 * </ul>
 *
 * <h3>缓存穿透防护</h3>
 * <p>当 DB 查询返回 null 时，缓存一个特殊标记（{@link #NULL_MARKER}），
 * TTL 较短（1 分钟），防止同一无效 ID 在短时间内反复穿透到 DB。
 * 配合布隆过滤器使用效果更佳——布隆过滤器拦截大部分无效 ID，
 * 空值缓存兜底布隆的误判。
 *
 * <h3>缓存雪崩防护</h3>
 * <p>所有缓存 TTL 在写入时随机偏移 ±10%，防止大量缓存同时过期导致
 * 瞬时 DB 压力。例如 24h TTL 实际过期时间在 21.6h ~ 26.4h 之间随机分布。
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /**
     * 空值标记——DB 中不存在的数据在缓存中的占位值。
     * 使用 JSON 格式便于与正常数据区分。
     */
    private static final String NULL_MARKER = "{\"__null__\":true}";

    /** 缓存 Key 前缀，统一命名空间，方便管理和监控 */
    private static final String KEY_PREFIX = "content:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // ========================================================================
    // 读缓存（简单类型，如 DestinationResponse）
    // ========================================================================

    /**
     * 读取缓存，未命中时从 DB 加载并回写缓存。
     *
     * <p>流程：
     * <ol>
     *   <li>查 Redis——命中且非空值标记则直接返回</li>
     *   <li>命中但为空值标记则返回 null（穿透防护生效）</li>
     *   <li>未命中则执行 {@code dbLoader} 查 DB</li>
     *   <li>DB 有数据 → 序列化写入 Redis（TTL 随机偏移防雪崩）</li>
     *   <li>DB 无数据 → 写入空值标记（短 TTL 防穿透）</li>
     * </ol>
     *
     * @param cacheKey 缓存 key（不含前缀，方法内部会加 "content:" 前缀）
     * @param type     目标类型（如 {@code DestinationResponse.class}）
     * @param dbLoader DB 查询函数，返回 null 表示数据不存在
     * @param ttl      缓存过期时间（会自动加 ±10% 随机偏移）
     * @param <T>      返回值类型
     * @return 缓存或 DB 中的数据，null 表示数据不存在
     */
    public <T> T getOrLoad(String cacheKey, Class<T> type, Supplier<T> dbLoader, Duration ttl) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        return doGetOrLoad(cacheKey, javaType, dbLoader, ttl);
    }

    // ========================================================================
    // 读缓存（泛型类型，如 Page<DestinationResponse>）
    // ========================================================================

    /**
     * 读取缓存（支持泛型类型），未命中时从 DB 加载并回写缓存。
     *
     * <p>用于返回类型为泛型（如 {@code Page<DestinationResponse>}）的场景，
     * 避免 Jackson 因类型擦除无法正确反序列化。
     *
     * <p>使用方式：
     * <pre>{@code
     * JavaType type = objectMapper.getTypeFactory()
     *     .constructParametricType(Page.class, DestinationResponse.class);
     * Page<DestinationResponse> result = cacheService.getOrLoad(key, type, loader, ttl);
     * }</pre>
     *
     * @param cacheKey 缓存 key（不含前缀）
     * @param javaType Jackson JavaType（含泛型信息）
     * @param dbLoader DB 查询函数
     * @param ttl      缓存过期时间
     * @param <T>      返回值类型
     * @return 缓存或 DB 中的数据
     */
    public <T> T getOrLoad(String cacheKey, JavaType javaType, Supplier<T> dbLoader, Duration ttl) {
        return doGetOrLoad(cacheKey, javaType, dbLoader, ttl);
    }

    /**
     * getOrLoad 的实际实现——查缓存 → 未命中则查 DB → 回写缓存。
     */
    private <T> T doGetOrLoad(String cacheKey, JavaType javaType, Supplier<T> dbLoader, Duration ttl) {
        String fullKey = KEY_PREFIX + cacheKey;

        // 1. 查缓存
        String json = stringRedisTemplate.opsForValue().get(fullKey);
        if (json != null) {
            if (NULL_MARKER.equals(json)) {
                // 命中空值缓存——数据在 DB 中也不存在，直接返回 null
                log.debug("缓存命中（空值标记）: key={}", fullKey);
                return null;
            }
            try {
                T data = objectMapper.readValue(json, javaType);
                log.debug("缓存命中: key={}", fullKey);
                return data;
            } catch (Exception e) {
                // 反序列化失败（可能是类结构变更导致旧缓存不可读），删除坏缓存后走 DB
                log.warn("缓存反序列化失败，删除坏缓存: key={}, error={}", fullKey, e.getMessage());
                stringRedisTemplate.delete(fullKey);
            }
        }

        // 2. 缓存未命中，查 DB
        log.debug("缓存未命中，查询DB: key={}", fullKey);
        T data = dbLoader.get();

        // 3. 回写缓存
        if (data != null) {
            // DB 有数据 → 序列化写入 Redis
            try {
                String value = objectMapper.writeValueAsString(data);
                Duration actualTtl = randomizeTtl(ttl);
                stringRedisTemplate.opsForValue().set(fullKey, value, actualTtl);
                log.debug("写入缓存: key={}, ttl={}s", fullKey, actualTtl.toSeconds());
            } catch (JsonProcessingException e) {
                // 序列化失败不影响业务流程——本次查询结果仍然返回给调用方
                log.error("缓存序列化失败: key={}, error={}", fullKey, e.getMessage(), e);
            }
        } else {
            // DB 无数据 → 写入空值标记（防穿透）
            // 短 TTL——布隆过滤器误判导致的无效 ID 查询应在 1 分钟后过期
            stringRedisTemplate.opsForValue().set(fullKey, NULL_MARKER, CacheConfig.NULL_VALUE_TTL);
            log.debug("写入空值缓存（防穿透）: key={}, ttl={}s",
                    fullKey, CacheConfig.NULL_VALUE_TTL.toSeconds());
        }

        return data;
    }

    // ========================================================================
    // 缓存失效（写操作后调用）
    // ========================================================================

    /**
     * 删除单个缓存 key。
     *
     * <p>在写操作（创建/更新/删除）后调用，保证下一次读取能获取到最新数据。
     * 采用"主动删除"而非"更新缓存"策略——因为更新操作可能涉及复杂的关联数据
     * （如更新目的地后关联景点列表也需要变更），删除后由下次读取时重新加载更可靠。
     *
     * @param cacheKey 缓存 key（不含 "content:" 前缀，方法内部会自动添加）
     */
    public void evict(String cacheKey) {
        String fullKey = KEY_PREFIX + cacheKey;
        stringRedisTemplate.delete(fullKey);
        log.debug("删除缓存: key={}", fullKey);
    }

    /**
     * 按模式批量删除缓存。
     *
     * <p>用于删除列表类缓存（如 {@code dest:list:*}），因为列表查询参数组合多样，
     * 无法精准删除每一个 key，使用通配符批量清除。
     *
     * <p>注意：{@code keys()} 命令在生产环境大数据量下可能阻塞 Redis，
     * 可后续升级为 {@code SCAN} 游标迭代。MVP 阶段数据量小直接使用即可。
     *
     * @param pattern 匹配模式（不含 "content:" 前缀），如 "dest:list:*"
     */
    public void evictByPattern(String pattern) {
        String fullPattern = KEY_PREFIX + pattern;
        Set<String> keys = stringRedisTemplate.keys(fullPattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.debug("批量删除缓存: pattern={}, count={}", fullPattern, keys.size());
        }
    }

    // ========================================================================
    // 内部工具方法
    // ========================================================================

    /**
     * TTL 随机偏移 ±10%，防止缓存雪崩。
     *
     * <p>原理：如果 100 个 key 都在同一秒过期，下一瞬间 100 个请求同时穿透到 DB，
     * 可能造成瞬时 DB 压力过大。加上随机偏移后，过期时间分散在 90%~110% TTL 区间内，
     * 自然将穿透请求分散到不同时间点。
     *
     * <p>示例：baseTtl=24h → 实际 TTL 在 21.6h ~ 26.4h 之间随机分布。
     *
     * @param baseTtl 基准过期时间
     * @return 加了随机偏移后的过期时间（基准值的 90% ~ 110%）
     */
    private Duration randomizeTtl(Duration baseTtl) {
        // ThreadLocalRandom：多线程环境下比 Math.random() 性能更好，无锁竞争
        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
        long millis = (long) (baseTtl.toMillis() * factor);
        return Duration.ofMillis(millis);
    }
}
