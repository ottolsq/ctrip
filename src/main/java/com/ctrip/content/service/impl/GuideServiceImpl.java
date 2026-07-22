package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.config.CacheConfig;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.converter.GuideConverter;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.GuideService;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 攻略服务实现类。
 *
 * <p>集成 Cache-Aside 缓存模式：读取时优先查缓存，未命中查 DB 并回写；
 * 写操作后主动删除缓存。攻略详情缓存 TTL 为 30 分钟，列表缓存为 5 分钟。
 *
 * <h3>浏览量一致性的取舍</h3>
 * <p>缓存命中时直接返回缓存数据，不更新浏览量（避免为每次浏览都穿透到 DB）。
 * 这意味着 {@code view_count} 在缓存有效期内最多有 30 分钟的延迟。
 * MVP 阶段此延迟可接受——用户感知不到分钟级的浏览数差异。
 *
 * <h3>缓存穿透防护</h3>
 * <ol>
 *   <li>布隆过滤器：查询前判断攻略 ID 是否为已发布状态</li>
 *   <li>空值缓存：DB 无数据时缓存空值标记（1 分钟 TTL），兜底布隆误判</li>
 * </ol>
 */
@Service
public class GuideServiceImpl implements GuideService {

    private static final Logger log = LoggerFactory.getLogger(GuideServiceImpl.class);

    /** 攻略详情缓存 Key 前缀 */
    private static final String CACHE_KEY_GUIDE = "guide:";

    /** 攻略列表缓存 Key 前缀 */
    private static final String CACHE_KEY_GUIDE_LIST = "guide:list:";

    private final GuideMapper guideMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final CacheService cacheService;
    private final RBloomFilter<String> guideBloomFilter;
    private final ObjectMapper objectMapper;

    public GuideServiceImpl(GuideMapper guideMapper,
                            DestinationMapper destinationMapper,
                            UserMapper userMapper,
                            CommentMapper commentMapper,
                            CacheService cacheService,
                            RBloomFilter<String> guideBloomFilter,
                            ObjectMapper objectMapper) {
        this.guideMapper = guideMapper;
        this.destinationMapper = destinationMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.cacheService = cacheService;
        this.guideBloomFilter = guideBloomFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GuideListResponse> listGuides(int page, int limit, Long destinationId,
                                               Long authorId, String keyword, String sortBy) {
        // 用参数构建缓存 key，相同查询条件命中同一缓存
        String cacheKey = CACHE_KEY_GUIDE_LIST
                + buildListCacheKey(page, limit, destinationId, authorId, keyword, sortBy);
        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, GuideListResponse.class);

        return cacheService.getOrLoad(cacheKey, pageType, () -> {
            Page<Guide> pageParam = new Page<>(page, limit);

            LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
            // 仅返回已发布的攻略
            wrapper.eq(Guide::getStatus, GuideStatus.PUBLISHED);

            if (destinationId != null) {
                wrapper.eq(Guide::getDestinationId, destinationId);
            }
            if (authorId != null) {
                wrapper.eq(Guide::getAuthorId, authorId);
            }
            if (keyword != null && !keyword.isBlank()) {
                wrapper.like(Guide::getTitle, keyword);
            }

            // 排序
            if ("view_count".equals(sortBy)) {
                wrapper.orderByDesc(Guide::getViewCount);
            } else if ("like_count".equals(sortBy)) {
                wrapper.orderByDesc(Guide::getLikeCount);
            } else {
                wrapper.orderByDesc(Guide::getCreatedAt);
            }

            Page<Guide> result = guideMapper.selectPage(pageParam, wrapper);
            List<Guide> guides = result.getRecords();

            // 批量查询作者信息
            Set<Long> authorIds = guides.stream()
                    .map(Guide::getAuthorId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, User> authorMap = batchQueryUsers(authorIds);

            // 批量查询目的地名称
            Set<Long> destinationIds = guides.stream()
                    .map(Guide::getDestinationId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> destinationNameMap = batchQueryDestinationNames(destinationIds);

            // 批量查询评论数量
            Map<Long, Integer> commentCountMap = batchQueryCommentCounts(guides.stream()
                    .map(Guide::getId)
                    .collect(Collectors.toSet()));

            // 转换为 DTO
            List<GuideListResponse> records = guides.stream()
                    .map(g -> {
                        User author = authorMap.get(g.getAuthorId());
                        String authorName = author != null ? author.getUsername() : null;
                        String authorAvatar = author != null ? author.getAvatarUrl() : null;
                        return GuideConverter.toListResponse(
                                g,
                                destinationNameMap.get(g.getDestinationId()),
                                authorName,
                                authorAvatar,
                                commentCountMap.getOrDefault(g.getId(), 0)
                        );
                    })
                    .collect(Collectors.toList());

            Page<GuideListResponse> responsePage = new Page<>(
                    result.getCurrent(), result.getSize(), result.getTotal());
            responsePage.setRecords(records);
            return responsePage;
        }, CacheConfig.GUIDE_LIST_TTL);
    }

    @Override
    @Transactional
    public GuideResponse getDetail(Long id) {
        // 第 1 层防护：布隆过滤器——仅已发布攻略的 ID 在过滤器中
        if (!guideBloomFilter.contains(CacheConfig.blogKey(id))) {
            log.debug("布隆过滤器拦截无效攻略ID: guideId={}", id);
            throw new ResourceNotFoundException("攻略不存在：id=" + id);
        }

        String cacheKey = CACHE_KEY_GUIDE + id;

        // 第 2 层：Cache-Aside 读取（含空值缓存防穿透）
        // 注意：缓存命中时不更新浏览量，view_count 最多有 30 分钟延迟
        GuideResponse cached = cacheService.getOrLoad(cacheKey, GuideResponse.class, () -> {
            // 缓存未命中——执行完整的 DB 查询流程（含浏览量更新）
            Guide guide = requirePublishedGuide(id);

            // 浏览量 +1
            guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                    .eq(Guide::getId, id)
                    .set(Guide::getViewCount, guide.getViewCount() + 1));

            Guide refreshedGuide = requirePublishedGuide(id);

            // 查询作者信息
            User author = userMapper.selectById(refreshedGuide.getAuthorId());
            String authorName = author != null ? author.getUsername() : null;
            String authorAvatar = author != null ? author.getAvatarUrl() : null;

            // 查询评论数量
            int commentCount = countComments(id);

            return GuideConverter.toResponse(
                    refreshedGuide,
                    resolveDestinationName(refreshedGuide.getDestinationId()),
                    authorName,
                    authorAvatar,
                    commentCount
            );
        }, CacheConfig.GUIDE_DETAIL_TTL);

        if (cached == null) {
            throw new ResourceNotFoundException("攻略不存在：id=" + id);
        }
        return cached;
    }

    @Override
    @Transactional
    public GuideResponse createGuide(Long authorId, CreateGuideRequest request) {
        Guide guide = GuideConverter.toEntity(authorId, request);
        guideMapper.insert(guide);

        // 如果发布状态为 PUBLISHED，加入布隆过滤器
        if (guide.getStatus() == GuideStatus.PUBLISHED) {
            guideBloomFilter.add(CacheConfig.blogKey(guide.getId()));
        }

        // 删除列表缓存（新攻略可能出现在列表中）
        cacheService.evictByPattern(CACHE_KEY_GUIDE_LIST + "*");

        User author = userMapper.selectById(authorId);
        String authorName = author != null ? author.getUsername() : null;
        String authorAvatar = author != null ? author.getAvatarUrl() : null;

        log.info("创建攻略: id={}, title={}, authorId={}", guide.getId(), guide.getTitle(), authorId);
        return GuideConverter.toResponse(
                requireGuide(guide.getId()),
                resolveDestinationName(guide.getDestinationId()),
                authorName,
                authorAvatar,
                0
        );
    }

    @Override
    @Transactional
    public GuideResponse updateGuide(Long id, Long userId, UpdateGuideRequest request) {
        Guide guide = requireGuide(id);
        checkGuideOwner(guide, userId);

        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .set(request.title() != null, Guide::getTitle, request.title())
                .set(request.content() != null, Guide::getContent, request.content())
                .set(request.destinationId() != null, Guide::getDestinationId, request.destinationId())
                .set(request.coverUrl() != null, Guide::getCoverUrl, request.coverUrl())
                .set(request.imageUrls() != null, Guide::getImageUrls, request.imageUrls())
                .set(request.status() != null, Guide::getStatus, request.status()));

        // 状态变更时同步更新布隆过滤器
        if (request.status() != null) {
            if (request.status() == GuideStatus.PUBLISHED) {
                guideBloomFilter.add(CacheConfig.blogKey(id));
            }
            // 状态从 PUBLISHED 改为其他时：
            // 布隆过滤器不支持删除操作，旧状态 ID 在下次应用重启重建时自然消失
        }

        // 删除详情缓存（数据已变更）
        cacheService.evict(CACHE_KEY_GUIDE + id);
        // 删除列表缓存（排序/筛选可能受影响）
        cacheService.evictByPattern(CACHE_KEY_GUIDE_LIST + "*");

        Guide updated = requireGuide(id);
        User author = userMapper.selectById(updated.getAuthorId());
        String authorName = author != null ? author.getUsername() : null;
        String authorAvatar = author != null ? author.getAvatarUrl() : null;
        int commentCount = countComments(id);

        log.info("更新攻略: id={}, title={}", id, updated.getTitle());
        return GuideConverter.toResponse(
                updated,
                resolveDestinationName(updated.getDestinationId()),
                authorName,
                authorAvatar,
                commentCount
        );
    }

    @Override
    @Transactional
    public void deleteGuide(Long id, Long userId) {
        Guide guide = requireGuide(id);
        checkGuideOwner(guide, userId);
        guideMapper.deleteById(id);

        // 删除详情缓存
        cacheService.evict(CACHE_KEY_GUIDE + id);
        // 删除列表缓存
        cacheService.evictByPattern(CACHE_KEY_GUIDE_LIST + "*");

        log.info("删除攻略: id={}", id);
        // 不主动从布隆过滤器移除（布隆过滤器不支持删除操作）
        // 下次应用重启时重建布隆过滤器，已删除的 ID 自然消失
    }

    @Override
    @Transactional
    public void likeGuide(Long id, Long userId) {
        requirePublishedGuide(id);
        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .setSql("like_count = like_count + 1"));

        // 点赞数变化 → 详情缓存失效（确保后续请求获取最新点赞数）
        cacheService.evict(CACHE_KEY_GUIDE + id);
    }

    @Override
    @Transactional
    public void unlikeGuide(Long id, Long userId) {
        requirePublishedGuide(id);
        // 确保不低于 0
        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .gt(Guide::getLikeCount, 0)
                .setSql("like_count = GREATEST(like_count - 1, 0)"));

        // 点赞数变化 → 详情缓存失效
        cacheService.evict(CACHE_KEY_GUIDE + id);
    }

    // --- 私有辅助（与改造前完全一致，保持不变） ---

    private Guide requireGuide(Long id) {
        Guide guide = guideMapper.selectById(id);
        if (guide == null) {
            throw new ResourceNotFoundException("攻略不存在：id=" + id);
        }
        return guide;
    }

    private Guide requirePublishedGuide(Long id) {
        Guide guide = requireGuide(id);
        if (guide.getStatus() != GuideStatus.PUBLISHED) {
            throw new ResourceNotFoundException("攻略不存在：id=" + id);
        }
        return guide;
    }

    /**
     * 校验操作权限：仅作者本人可操作。
     */
    private void checkGuideOwner(Guide guide, Long userId) {
        if (!guide.getAuthorId().equals(userId)) {
            throw new AuthenticationException("无权操作他人攻略");
        }
    }

    /**
     * 解析目的地名称（可为 null）。
     */
    private String resolveDestinationName(Long destinationId) {
        if (destinationId == null) {
            return null;
        }
        Destination dest = destinationMapper.selectById(destinationId);
        return dest != null ? dest.getName() : null;
    }

    /**
     * 批量查询用户信息。
     */
    private Map<Long, User> batchQueryUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    /**
     * 批量查询目的地名称。
     */
    private Map<Long, String> batchQueryDestinationNames(Set<Long> destinationIds) {
        if (destinationIds.isEmpty()) {
            return Map.of();
        }
        List<Destination> destinations = destinationMapper.selectBatchIds(destinationIds);
        return destinations.stream()
                .collect(Collectors.toMap(Destination::getId, Destination::getName, (a, b) -> a));
    }

    /**
     * 批量查询评论数量。
     */
    private Map<Long, Integer> batchQueryCommentCounts(Set<Long> guideIds) {
        if (guideIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Comment::getGuideId, guideIds);
        List<Comment> comments = commentMapper.selectList(wrapper);

        // 按 guideId 分组计数
        return comments.stream()
                .collect(Collectors.groupingBy(Comment::getGuideId, Collectors.summingInt(c -> 1)));
    }

    /**
     * 查询单个攻略的评论数量。
     */
    private int countComments(Long guideId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getGuideId, guideId);
        return Math.toIntExact(commentMapper.selectCount(wrapper));
    }

    /**
     * 根据列表查询参数构建缓存 key 后缀。
     *
     * <p>将查询参数拼接为唯一字符串后取 hashCode，相同参数的查询命中同一缓存。
     */
    private String buildListCacheKey(int page, int limit, Long destinationId,
                                      Long authorId, String keyword, String sortBy) {
        String params = String.format("p=%d&l=%d&d=%s&a=%s&k=%s&s=%s",
                page, limit,
                destinationId == null ? "" : destinationId,
                authorId == null ? "" : authorId,
                keyword == null ? "" : keyword,
                sortBy == null ? "default" : sortBy);
        return String.valueOf(params.hashCode());
    }
}
