package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.converter.GuideConverter;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.GuideService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略服务实现类。
 */
@Service
public class GuideServiceImpl implements GuideService {

    private final GuideMapper guideMapper;
    private final DestinationMapper destinationMapper;

    public GuideServiceImpl(GuideMapper guideMapper, DestinationMapper destinationMapper) {
        this.guideMapper = guideMapper;
        this.destinationMapper = destinationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GuideListResponse> listGuides(int page, int limit, Long destinationId, Long authorId, String keyword, String sortBy) {
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

        // 转换为 DTO（批量查询目的地名称）
        List<GuideListResponse> records = result.getRecords().stream()
                .map(g -> GuideConverter.toListResponse(g, resolveDestinationName(g.getDestinationId())))
                .collect(Collectors.toList());

        Page<GuideListResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional
    public GuideResponse getDetail(Long id) {
        Guide guide = requirePublishedGuide(id);

        // 浏览量 +1
        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .set(Guide::getViewCount, guide.getViewCount() + 1));

        return GuideConverter.toResponse(requirePublishedGuide(id), resolveDestinationName(guide.getDestinationId()));
    }

    @Override
    @Transactional
    public GuideResponse createGuide(Long authorId, CreateGuideRequest request) {
        Guide guide = GuideConverter.toEntity(authorId, request);
        guideMapper.insert(guide);
        return GuideConverter.toResponse(requireGuide(guide.getId()), resolveDestinationName(guide.getDestinationId()));
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

        Guide updated = requireGuide(id);
        return GuideConverter.toResponse(updated, resolveDestinationName(updated.getDestinationId()));
    }

    @Override
    @Transactional
    public void deleteGuide(Long id, Long userId) {
        Guide guide = requireGuide(id);
        checkGuideOwner(guide, userId);
        guideMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void likeGuide(Long id) {
        requirePublishedGuide(id);
        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .setSql("like_count = like_count + 1"));
    }

    @Override
    @Transactional
    public void unlikeGuide(Long id) {
        requirePublishedGuide(id);
        // 确保不低于 0
        guideMapper.update(null, new LambdaUpdateWrapper<Guide>()
                .eq(Guide::getId, id)
                .gt(Guide::getLikeCount, 0)
                .setSql("like_count = GREATEST(like_count - 1, 0)"));
    }

    // --- 私有辅助 ---

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
}
