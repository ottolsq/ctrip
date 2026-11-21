package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.common.tx.AfterCommitExecutor;
import com.ctrip.config.CacheConfig;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.converter.GuideConverter;
import com.ctrip.content.dto.response.AdminGuideListItemResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.AdminGuideService;
import com.ctrip.messaging.publisher.EventPublisher;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端攻略审核服务实现。
 */
@Service
public class AdminGuideServiceImpl implements AdminGuideService {

    private static final Logger log = LoggerFactory.getLogger(AdminGuideServiceImpl.class);

    private static final String CACHE_KEY_GUIDE = "guide:";
    private static final String CACHE_KEY_GUIDE_LIST = "guide:list:";

    private final GuideMapper guideMapper;
    private final UserMapper userMapper;
    private final DestinationMapper destinationMapper;
    private final CommentMapper commentMapper;
    private final CacheService cacheService;
    private final RBloomFilter<String> guideBloomFilter;
    private final EventPublisher eventPublisher;

    public AdminGuideServiceImpl(GuideMapper guideMapper,
                                 UserMapper userMapper,
                                 DestinationMapper destinationMapper,
                                 CommentMapper commentMapper,
                                 CacheService cacheService,
                                 RBloomFilter<String> guideBloomFilter,
                                 EventPublisher eventPublisher) {
        this.guideMapper = guideMapper;
        this.userMapper = userMapper;
        this.destinationMapper = destinationMapper;
        this.commentMapper = commentMapper;
        this.cacheService = cacheService;
        this.guideBloomFilter = guideBloomFilter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminGuideListItemResponse> listGuides(int page, int size, GuideStatus status,
                                                       String keyword, Long authorId) {
        int effectiveSize = Math.min(Math.max(size, 1), 100);
        Page<Guide> pageObj = new Page<>(page, effectiveSize);

        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Guide::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Guide::getTitle, keyword);
        }
        if (authorId != null) {
            wrapper.eq(Guide::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Guide::getUpdatedAt);

        Page<Guide> result = guideMapper.selectPage(pageObj, wrapper);
        Set<Long> authorIds = result.getRecords().stream()
                .map(Guide::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> authorNames = new HashMap<>();
        if (!authorIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(authorIds)) {
                authorNames.put(user.getId(), user.getUsername());
            }
        }

        Page<AdminGuideListItemResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream()
                .map(guide -> new AdminGuideListItemResponse(
                        guide.getId(),
                        guide.getAuthorId(),
                        authorNames.get(guide.getAuthorId()),
                        guide.getTitle(),
                        guide.getStatus() != null ? guide.getStatus().name() : null,
                        guide.getRejectReason(),
                        guide.getViewCount(),
                        guide.getLikeCount(),
                        guide.getCreatedAt(),
                        guide.getUpdatedAt()))
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public GuideResponse getDetail(Long guideId) {
        return buildGuideResponse(requireGuide(guideId));
    }

    @Override
    @Transactional
    public void approve(Long guideId, Long operatorId) {
        Guide guide = requirePending(guideId);

        Guide update = Guide.builder()
                .id(guideId)
                .status(GuideStatus.PUBLISHED)
                .rejectReason(null)
                .reviewedBy(operatorId)
                .reviewedAt(LocalDateTime.now())
                .build();
        guideMapper.updateById(update);

        AfterCommitExecutor.run(() -> {
            guideBloomFilter.add(CacheConfig.blogKey(guideId));
            evictGuideCaches(guideId);
            eventPublisher.publishContentAuditEvent(
                    "GUIDE_APPROVED", guideId, guide.getAuthorId(), operatorId, null);
        });

        log.info("攻略审核通过: guideId={}, operatorId={}", guideId, operatorId);
    }

    @Override
    @Transactional
    public void reject(Long guideId, Long operatorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("驳回理由不能为空");
        }
        Guide guide = requirePending(guideId);
        String trimmedReason = reason.trim();

        Guide update = Guide.builder()
                .id(guideId)
                .status(GuideStatus.REJECTED)
                .rejectReason(trimmedReason)
                .reviewedBy(operatorId)
                .reviewedAt(LocalDateTime.now())
                .build();
        guideMapper.updateById(update);

        AfterCommitExecutor.run(() -> {
            evictGuideCaches(guideId);
            eventPublisher.publishContentAuditEvent(
                    "GUIDE_REJECTED", guideId, guide.getAuthorId(), operatorId, trimmedReason);
        });

        log.info("攻略审核驳回: guideId={}, operatorId={}", guideId, operatorId);
    }

    private Guide requireGuide(Long guideId) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null) {
            throw new ResourceNotFoundException("攻略不存在：id=" + guideId);
        }
        return guide;
    }

    private Guide requirePending(Long guideId) {
        Guide guide = requireGuide(guideId);
        if (guide.getStatus() != GuideStatus.PENDING) {
            throw new BusinessException("仅待审核攻略可执行审核操作");
        }
        return guide;
    }

    private GuideResponse buildGuideResponse(Guide guide) {
        User author = userMapper.selectById(guide.getAuthorId());
        String authorName = author != null ? author.getUsername() : null;
        String authorAvatar = author != null ? author.getAvatarUrl() : null;
        String destinationName = null;
        if (guide.getDestinationId() != null) {
            Destination destination = destinationMapper.selectById(guide.getDestinationId());
            destinationName = destination != null ? destination.getName() : null;
        }
        long commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getGuideId, guide.getId()));
        return GuideConverter.toResponse(
                guide, destinationName, authorName, authorAvatar, Math.toIntExact(commentCount));
    }

    private void evictGuideCaches(Long guideId) {
        cacheService.evict(CACHE_KEY_GUIDE + guideId);
        cacheService.evictByPattern(CACHE_KEY_GUIDE_LIST + "*");
    }
}
