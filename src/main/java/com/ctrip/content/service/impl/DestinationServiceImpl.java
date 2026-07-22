package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.config.CacheConfig;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.converter.AttractionConverter;
import com.ctrip.content.converter.DestinationConverter;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.dto.response.DestinationResponse;
import com.ctrip.content.entity.Attraction;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.mapper.AttractionMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.service.DestinationService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 目的地服务实现类。
 *
 * <p>集成 Cache-Aside 缓存模式：读取时优先查缓存，未命中查 DB 并回写；
 * 写操作后主动删除缓存以保证数据一致性。
 *
 * <h3>缓存穿透防护</h3>
 * <ol>
 *   <li>布隆过滤器：查询前先判断 ID 是否有效，拦截绝大多数无效 ID</li>
 *   <li>空值缓存：DB 无数据时缓存空值标记（1 分钟 TTL），兜底布隆误判</li>
 *   <li>参数校验：Controller 层校验 id > 0，拦截非法输入</li>
 * </ol>
 */
@Service
public class DestinationServiceImpl implements DestinationService {

    private static final Logger log = LoggerFactory.getLogger(DestinationServiceImpl.class);

    /** 目的地详情缓存 Key 前缀 */
    private static final String CACHE_KEY_DEST = "dest:";

    /** 目的地列表缓存 Key 前缀 */
    private static final String CACHE_KEY_DEST_LIST = "dest:list:";

    private final DestinationMapper destinationMapper;
    private final AttractionMapper attractionMapper;
    private final CacheService cacheService;
    private final RBloomFilter<String> destinationBloomFilter;
    private final ObjectMapper objectMapper;

    public DestinationServiceImpl(DestinationMapper destinationMapper,
                                   AttractionMapper attractionMapper,
                                   CacheService cacheService,
                                   RBloomFilter<String> destinationBloomFilter,
                                   ObjectMapper objectMapper) {
        this.destinationMapper = destinationMapper;
        this.attractionMapper = attractionMapper;
        this.cacheService = cacheService;
        this.destinationBloomFilter = destinationBloomFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DestinationResponse> listDestinations(int page, int limit, String country,
                                                       String province, String keyword) {
        // 用参数构建缓存 key（含分页信息），相同查询条件命中同一缓存
        String cacheKey = CACHE_KEY_DEST_LIST + buildListCacheKey(page, limit, country, province, keyword);
        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, DestinationResponse.class);

        // 列表缓存 TTL 较短（5 分钟），因为列表数据变动更频繁
        return cacheService.getOrLoad(cacheKey, pageType, () -> {
            Page<Destination> pageParam = new Page<>(page, limit);

            LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
            if (country != null && !country.isBlank()) {
                wrapper.eq(Destination::getCountry, country);
            }
            if (province != null && !province.isBlank()) {
                wrapper.eq(Destination::getProvince, province);
            }
            if (keyword != null && !keyword.isBlank()) {
                wrapper.like(Destination::getName, keyword);
            }
            wrapper.orderByDesc(Destination::getCreatedAt);

            Page<Destination> result = destinationMapper.selectPage(pageParam, wrapper);

            // 转换为 DTO
            List<DestinationResponse> records = result.getRecords().stream()
                    .map(DestinationConverter::toResponse)
                    .collect(Collectors.toList());

            // 构建分页结果
            Page<DestinationResponse> responsePage = new Page<>(
                    result.getCurrent(), result.getSize(), result.getTotal());
            responsePage.setRecords(records);
            return responsePage;
        }, CacheConfig.DEST_LIST_TTL);
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationResponse getDetail(Long id) {
        // 第 1 层防护：布隆过滤器——判断 ID 是否有效
        // 布隆过滤器说"不存在"一定正确，说"可能存在"才继续查
        if (!destinationBloomFilter.contains(CacheConfig.bloomKey(id))) {
            log.debug("布隆过滤器拦截无效ID: destId={}", id);
            throw new ResourceNotFoundException("目的地不存在：id=" + id);
        }

        String cacheKey = CACHE_KEY_DEST + id;

        // 第 2 层防护：Cache-Aside 读取（含空值缓存防穿透）
        DestinationResponse cached = cacheService.getOrLoad(cacheKey, DestinationResponse.class, () -> {
            // 缓存未命中——查 DB
            Destination destination = destinationMapper.selectById(id);
            if (destination == null) {
                // 布隆过滤器误判——返回 null，CacheService 会自动写空值缓存
                return null;
            }

            // 查询关联景点
            LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Attraction::getDestinationId, id);
            List<AttractionResponse> attractions = attractionMapper.selectList(wrapper).stream()
                    .map(AttractionConverter::toResponse)
                    .collect(Collectors.toList());

            return DestinationConverter.toResponse(destination, attractions);
        }, CacheConfig.DEST_DETAIL_TTL);

        if (cached == null) {
            throw new ResourceNotFoundException("目的地不存在：id=" + id);
        }
        return cached;
    }

    @Override
    @Transactional
    public DestinationResponse createDestination(CreateDestinationRequest request) {
        Destination destination = DestinationConverter.toEntity(request);
        destinationMapper.insert(destination);

        // 新 ID 加入布隆过滤器，后续查询可被识别为有效 ID
        destinationBloomFilter.add(CacheConfig.bloomKey(destination.getId()));

        // 删除列表缓存——新数据会使列表结果变化
        cacheService.evictByPattern(CACHE_KEY_DEST_LIST + "*");

        log.info("创建目的地: id={}, name={}", destination.getId(), destination.getName());
        return getDetail(destination.getId());
    }

    @Override
    @Transactional
    public DestinationResponse updateDestination(Long id, UpdateDestinationRequest request) {
        requireDestination(id);

        destinationMapper.update(null, new LambdaUpdateWrapper<Destination>()
                .eq(Destination::getId, id)
                .set(request.name() != null, Destination::getName, request.name())
                .set(request.country() != null, Destination::getCountry, request.country())
                .set(request.province() != null, Destination::getProvince, request.province())
                .set(request.description() != null, Destination::getDescription, request.description())
                .set(request.bestSeason() != null, Destination::getBestSeason, request.bestSeason())
                .set(request.coverUrl() != null, Destination::getCoverUrl, request.coverUrl())
                .set(request.imageUrls() != null, Destination::getImageUrls, request.imageUrls()));

        // 删除详情缓存（数据已变更，下次读取需重新加载）
        cacheService.evict(CACHE_KEY_DEST + id);
        // 删除列表缓存（列表排序/筛选结果可能受更新影响）
        cacheService.evictByPattern(CACHE_KEY_DEST_LIST + "*");

        log.info("更新目的地: id={}", id);
        return getDetail(id);
    }

    @Override
    @Transactional
    public void deleteDestination(Long id) {
        requireDestination(id);
        destinationMapper.deleteById(id);

        // 删除详情缓存
        cacheService.evict(CACHE_KEY_DEST + id);
        // 删除列表缓存
        cacheService.evictByPattern(CACHE_KEY_DEST_LIST + "*");

        log.info("删除目的地: id={}", id);
        // 注意：不主动从布隆过滤器移除（布隆过滤器不支持删除操作）
        // 被删除的 ID 若被查询：布隆说"可能存在"→ 缓存未命中 → DB 查询返回 null
        // → CacheService 写空值缓存（1分钟），后续请求走相同流程
        // 下次应用重启时布隆过滤器重建，旧 ID 自然消失
    }

    /**
     * 查询目的地（直接查 DB，绕过缓存），不存在则抛出异常。
     *
     * <p>此方法仅用于写操作前的存在性校验——写操作需要保证数据一致性，
     * 不应依赖缓存（缓存可能已过期或与 DB 不一致）。
     */
    private Destination requireDestination(Long id) {
        Destination destination = destinationMapper.selectById(id);
        if (destination == null) {
            throw new ResourceNotFoundException("目的地不存在：id=" + id);
        }
        return destination;
    }

    /**
     * 根据列表查询参数构建缓存 key 后缀。
     *
     * <p>将查询参数拼接为唯一字符串，相同参数的查询命中同一缓存。
     * 使用 String.hashCode() 控制 key 长度，避免过长的 Redis key。
     */
    private String buildListCacheKey(int page, int limit, String country,
                                      String province, String keyword) {
        String params = String.format("p=%d&l=%d&c=%s&pr=%s&k=%s",
                page, limit,
                country == null ? "" : country,
                province == null ? "" : province,
                keyword == null ? "" : keyword);
        return String.valueOf(params.hashCode());
    }
}
