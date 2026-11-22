package com.ctrip.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.operation.dto.PublicRecommendationResponse;
import com.ctrip.operation.dto.RecommendationRequest;
import com.ctrip.operation.dto.RecommendationResponse;
import com.ctrip.operation.entity.Recommendation;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.entity.enums.RecommendationTargetType;
import com.ctrip.operation.mapper.RecommendationMapper;
import com.ctrip.operation.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐位服务实现。
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RecommendationMapper recommendationMapper;
    private final GuideMapper guideMapper;
    private final DestinationMapper destinationMapper;
    private final BlindBoxTemplateMapper templateMapper;

    public RecommendationServiceImpl(RecommendationMapper recommendationMapper,
                                     GuideMapper guideMapper,
                                     DestinationMapper destinationMapper,
                                     BlindBoxTemplateMapper templateMapper) {
        this.recommendationMapper = recommendationMapper;
        this.guideMapper = guideMapper;
        this.destinationMapper = destinationMapper;
        this.templateMapper = templateMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecommendationResponse> listRecommendations(int page, int size, String slot,
                                                            DisplayStatus status) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Recommendation> wrapper = new LambdaQueryWrapper<>();
        if (slot != null && !slot.isBlank()) {
            wrapper.eq(Recommendation::getSlot, slot);
        }
        if (status != null) {
            wrapper.eq(Recommendation::getStatus, status);
        }
        wrapper.orderByAsc(Recommendation::getSortOrder).orderByAsc(Recommendation::getId);

        Page<Recommendation> result = recommendationMapper.selectPage(
                new Page<>(page, effectiveSize), wrapper);
        Page<RecommendationResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicRecommendationResponse> listActive(String slot, LocalDateTime now) {
        if (slot == null || slot.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<Recommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recommendation::getSlot, slot)
                .eq(Recommendation::getStatus, DisplayStatus.ENABLED);

        List<Recommendation> active = recommendationMapper.selectList(wrapper).stream()
                .filter(item -> item.getStatus() == DisplayStatus.ENABLED)
                .filter(item -> item.getStartAt() == null || !now.isBefore(item.getStartAt()))
                .filter(item -> item.getEndAt() == null || !now.isAfter(item.getEndAt()))
                .sorted(Comparator.comparing(
                                (Recommendation item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(item -> item.getId() == null ? 0L : item.getId()))
                .toList();

        Map<Long, Guide> guides = guideIds(active).isEmpty() ? Map.of()
                : guideMapper.selectBatchIds(guideIds(active)).stream()
                        .collect(Collectors.toMap(Guide::getId, Function.identity(), (a, b) -> a));
        Map<Long, Destination> destinations = destinationIds(active).isEmpty() ? Map.of()
                : destinationMapper.selectBatchIds(destinationIds(active)).stream()
                        .collect(Collectors.toMap(Destination::getId, Function.identity(), (a, b) -> a));
        Map<Long, BlindBoxTemplate> templates = templateIds(active).isEmpty() ? Map.of()
                : templateMapper.selectBatchIds(templateIds(active)).stream()
                        .collect(Collectors.toMap(BlindBoxTemplate::getId, Function.identity(), (a, b) -> a));

        return active.stream()
                .map(item -> assemblePublic(item, guides, destinations, templates))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public RecommendationResponse create(RecommendationRequest request) {
        validateWindow(request.startAt(), request.endAt());
        validateTarget(request.targetType(), request.targetId());

        Recommendation recommendation = Recommendation.builder()
                .slot(request.slot())
                .title(request.title())
                .targetType(request.targetType())
                .targetId(request.targetId())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .status(request.status() == null ? DisplayStatus.ENABLED : request.status())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
        recommendationMapper.insert(recommendation);
        return toResponse(recommendation);
    }

    @Override
    @Transactional
    public RecommendationResponse update(Long id, RecommendationRequest request) {
        Recommendation existing = requireRecommendation(id);
        validateWindow(request.startAt(), request.endAt());
        validateTarget(request.targetType(), request.targetId());

        existing.setSlot(request.slot());
        existing.setTitle(request.title());
        existing.setTargetType(request.targetType());
        existing.setTargetId(request.targetId());
        existing.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        existing.setStatus(request.status() == null ? existing.getStatus() : request.status());
        existing.setStartAt(request.startAt());
        existing.setEndAt(request.endAt());
        if (recommendationMapper.updateById(existing) == 0) {
            throw new BusinessException("推荐位已被他人修改，请刷新后重试");
        }
        return toResponse(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRecommendation(id);
        recommendationMapper.deleteById(id);
    }

    private void validateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BusinessException("展示开始时间必须早于结束时间");
        }
    }

    private void validateTarget(RecommendationTargetType type, Long targetId) {
        boolean exists = switch (type) {
            case GUIDE -> guideMapper.selectById(targetId) != null;
            case DESTINATION -> destinationMapper.selectById(targetId) != null;
            case BLIND_BOX_TEMPLATE -> templateMapper.selectById(targetId) != null;
        };
        if (!exists) {
            throw new BusinessException("推荐目标不存在");
        }
    }

    private PublicRecommendationResponse assemblePublic(
            Recommendation item, Map<Long, Guide> guides,
            Map<Long, Destination> destinations,
            Map<Long, BlindBoxTemplate> templates) {
        return switch (item.getTargetType()) {
            case GUIDE -> {
                Guide guide = guides.get(item.getTargetId());
                if (guide == null || guide.getStatus() != GuideStatus.PUBLISHED) {
                    yield null;
                }
                yield new PublicRecommendationResponse(
                        item.getId(), item.getSlot(), item.getTitle(), "GUIDE", guide.getId(),
                        guide.getTitle(), guide.getCoverUrl(), "/guides/" + guide.getId());
            }
            case DESTINATION -> {
                Destination destination = destinations.get(item.getTargetId());
                if (destination == null) {
                    yield null;
                }
                yield new PublicRecommendationResponse(
                        item.getId(), item.getSlot(), item.getTitle(), "DESTINATION",
                        destination.getId(), destination.getName(), destination.getCoverUrl(),
                        "/destinations/" + destination.getId());
            }
            case BLIND_BOX_TEMPLATE -> {
                BlindBoxTemplate template = templates.get(item.getTargetId());
                if (template == null || template.getStatus() != TemplateStatus.ACTIVE) {
                    yield null;
                }
                yield new PublicRecommendationResponse(
                        item.getId(), item.getSlot(), item.getTitle(), "BLIND_BOX_TEMPLATE",
                        template.getId(), template.getName(), null,
                        "/blind-box/" + template.getId());
            }
        };
    }

    private java.util.Set<Long> guideIds(List<Recommendation> items) {
        return targetIds(items, RecommendationTargetType.GUIDE);
    }

    private java.util.Set<Long> destinationIds(List<Recommendation> items) {
        return targetIds(items, RecommendationTargetType.DESTINATION);
    }

    private java.util.Set<Long> templateIds(List<Recommendation> items) {
        return targetIds(items, RecommendationTargetType.BLIND_BOX_TEMPLATE);
    }

    private java.util.Set<Long> targetIds(List<Recommendation> items,
                                          RecommendationTargetType type) {
        return items.stream()
                .filter(item -> item.getTargetType() == type)
                .map(Recommendation::getTargetId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Recommendation requireRecommendation(Long id) {
        Recommendation recommendation = recommendationMapper.selectById(id);
        if (recommendation == null) {
            throw new ResourceNotFoundException("推荐位不存在：id=" + id);
        }
        return recommendation;
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getSlot(),
                recommendation.getTitle(),
                recommendation.getTargetType() != null ? recommendation.getTargetType().name() : null,
                recommendation.getTargetId(),
                recommendation.getSortOrder(),
                recommendation.getStatus() != null ? recommendation.getStatus().name() : null,
                recommendation.getStartAt(),
                recommendation.getEndAt(),
                recommendation.getCreatedAt()
        );
    }
}
