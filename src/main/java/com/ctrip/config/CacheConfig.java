package com.ctrip.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 缓存配置类。
 *
 * <p>定义缓存 TTL 常量，初始化布隆过滤器（Redisson RBloomFilter），
 * 防止缓存穿透——对不存在的数据 ID 在查询缓存 / DB 之前就被拦截。
 *
 * <p>布隆过滤器在应用启动时从数据库加载全部有效 ID，后续新增 ID
 * 在 Service 层写操作时动态添加。
 */
@Configuration
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    // ========================================================================
    // 缓存 TTL 常量（均会加 ±10% 随机偏移，防止缓存雪崩）
    // ========================================================================

    /** 目的地详情缓存 TTL：24 小时 */
    public static final Duration DEST_DETAIL_TTL = Duration.ofHours(24);

    /** 目的地列表缓存 TTL：5 分钟（列表数据变化相对频繁） */
    public static final Duration DEST_LIST_TTL = Duration.ofMinutes(5);

    /** 攻略详情缓存 TTL：30 分钟 */
    public static final Duration GUIDE_DETAIL_TTL = Duration.ofMinutes(30);

    /** 攻略列表缓存 TTL：5 分钟 */
    public static final Duration GUIDE_LIST_TTL = Duration.ofMinutes(5);

    /** 目的地关联景点列表缓存 TTL：1 小时 */
    public static final Duration ATTRACTION_BY_DEST_TTL = Duration.ofHours(1);

    /** 空值缓存 TTL：1 分钟（防穿透关键——DB 中不存在的数据也缓存，但时间短） */
    public static final Duration NULL_VALUE_TTL = Duration.ofMinutes(1);

    // ========================================================================
    // 布隆过滤器 Key 常量
    // ========================================================================

    /** 目的地 ID 布隆过滤器 Redis Key */
    private static final String BLOOM_DEST_KEY = "bloom:dest:ids";

    /** 攻略 ID 布隆过滤器 Redis Key */
    private static final String BLOOM_GUIDE_KEY = "bloom:guide:ids";

    // ========================================================================
    // 布隆过滤器参数
    // ========================================================================

    /** 目的地布隆过滤器预期元素数量 */
    private static final long BLOOM_DEST_EXPECTED = 10_000L;

    /** 攻略布隆过滤器预期元素数量 */
    private static final long BLOOM_GUIDE_EXPECTED = 50_000L;

    /** 布隆过滤器误判率：1%（误判仅导致一次多余的缓存+DB查询，影响很小） */
    private static final double BLOOM_FALSE_PROBABILITY = 0.01;

    private final RedissonClient redissonClient;
    private final DestinationMapper destinationMapper;
    private final GuideMapper guideMapper;

    public CacheConfig(RedissonClient redissonClient,
                       DestinationMapper destinationMapper,
                       GuideMapper guideMapper) {
        this.redissonClient = redissonClient;
        this.destinationMapper = destinationMapper;
        this.guideMapper = guideMapper;
    }

    // ========================================================================
    // 布隆过滤器 Bean
    // ========================================================================

    /**
     * 目的地 ID 布隆过滤器。
     *
     * <p>存储格式：{@code "dest:{id}"}，如 "dest:123"。
     * 应用启动时通过 {@link #initBloomFilters()} 加载全部有效 ID。
     *
     * @return Redisson RBloomFilter 实例
     */
    @Bean
    public RBloomFilter<String> destinationBloomFilter() {
        RBloomFilter<String> filter = redissonClient.getBloomFilter(BLOOM_DEST_KEY);
        filter.tryInit(BLOOM_DEST_EXPECTED, BLOOM_FALSE_PROBABILITY);
        return filter;
    }

    /**
     * 攻略 ID 布隆过滤器。
     *
     * <p>仅存储已发布（PUBLISHED）状态的攻略 ID，格式为 {@code "guide:{id}"}。
     *
     * @return Redisson RBloomFilter 实例
     */
    @Bean
    public RBloomFilter<String> guideBloomFilter() {
        RBloomFilter<String> filter = redissonClient.getBloomFilter(BLOOM_GUIDE_KEY);
        filter.tryInit(BLOOM_GUIDE_EXPECTED, BLOOM_FALSE_PROBABILITY);
        return filter;
    }

    // ========================================================================
    // 启动时初始化：从数据库加载全部有效 ID 到布隆过滤器
    // ========================================================================

    /**
     * 应用启动就绪后，从数据库查询所有有效 ID 并写入布隆过滤器。
     *
     * <p>使用 {@code ApplicationReadyEvent} 而非 {@code @PostConstruct}，
     * 避免与 {@code @Configuration} 的 CGLIB 代理产生循环依赖。
     * 此时所有 Bean（包括 RBloomFilter）已完全初始化，可安全调用。
     *
     * <p>目的：保证布隆过滤器在应用启动后立即可用，不需要等待
     * "首次查询 → 缓存未命中 → 查 DB" 的预热过程。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initBloomFilters() {
        initDestinationBloomFilter();
        initGuideBloomFilter();
    }

    /**
     * 加载全部目的地 ID 到布隆过滤器。
     *
     * <p>只查 id 字段，避免全表数据加载到内存。
     */
    private void initDestinationBloomFilter() {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Destination::getId);
        List<Destination> destinations = destinationMapper.selectList(wrapper);

        RBloomFilter<String> filter = destinationBloomFilter();
        for (Destination dest : destinations) {
            filter.add(bloomKey(dest.getId()));
        }
        log.info("目的地布隆过滤器初始化完成：加载 {} 个 ID", destinations.size());
    }

    /**
     * 加载全部已发布攻略 ID 到布隆过滤器。
     *
     * <p>只加载已发布状态——草稿和已拒绝的攻略不应被公开访问，无需缓存防护。
     */
    private void initGuideBloomFilter() {
        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Guide::getId);
        wrapper.eq(Guide::getStatus, GuideStatus.PUBLISHED);
        List<Guide> guides = guideMapper.selectList(wrapper);

        RBloomFilter<String> filter = guideBloomFilter();
        for (Guide guide : guides) {
            filter.add(blogKey(guide.getId()));
        }
        log.info("攻略布隆过滤器初始化完成：加载 {} 个已发布 ID", guides.size());
    }

    // ========================================================================
    // 布隆过滤器 Key 构建工具方法
    // ========================================================================

    /**
     * 构建目的地布隆过滤器 key。
     *
     * @param destId 目的地 ID
     * @return 布隆过滤器存储的 key，如 "dest:123"
     */
    public static String bloomKey(Long destId) {
        return "dest:" + destId;
    }

    /**
     * 构建攻略布隆过滤器 key。
     *
     * @param guideId 攻略 ID
     * @return 布隆过滤器存储的 key，如 "guide:456"
     */
    public static String blogKey(Long guideId) {
        return "guide:" + guideId;
    }
}
