package com.ctrip.itinerary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.itinerary.dto.response.CollectionItemResponse;
import com.ctrip.itinerary.entity.Collection;
import com.ctrip.itinerary.entity.Itinerary;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import com.ctrip.itinerary.mapper.CollectionMapper;
import com.ctrip.itinerary.mapper.ItineraryMapper;
import com.ctrip.itinerary.service.CollectionService;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用收藏服务实现。
 *
 * <p>复用 collection 表与 Collection 实体，统一处理攻略/行程收藏。
 */
@Service
public class CollectionServiceImpl implements CollectionService {

    private static final int SUMMARY_MAX_LENGTH = 120;

    private final CollectionMapper collectionMapper;
    private final GuideMapper guideMapper;
    private final ItineraryMapper itineraryMapper;
    private final UserMapper userMapper;
    private final DestinationMapper destinationMapper;

    public CollectionServiceImpl(CollectionMapper collectionMapper,
                                 GuideMapper guideMapper,
                                 ItineraryMapper itineraryMapper,
                                 UserMapper userMapper,
                                 DestinationMapper destinationMapper) {
        this.collectionMapper = collectionMapper;
        this.guideMapper = guideMapper;
        this.itineraryMapper = itineraryMapper;
        this.userMapper = userMapper;
        this.destinationMapper = destinationMapper;
    }

    @Override
    @Transactional
    public Page<CollectionItemResponse> listCollections(Long userId, int page, int size,
                                                        CollectionTargetType type) {
        if (type == CollectionTargetType.DESTINATION) {
            return new Page<>(page, size);
        }

        Page<Collection> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId);
        if (type != null) {
            wrapper.eq(Collection::getTargetType, type);
        }
        wrapper.orderByDesc(Collection::getCreatedAt);

        Page<Collection> result = collectionMapper.selectPage(pageObj, wrapper);
        List<Collection> collections = result.getRecords();

        List<Long> guideIds = collections.stream()
                .filter(c -> c.getTargetType() == CollectionTargetType.GUIDE)
                .map(Collection::getTargetId).distinct().toList();
        List<Long> itineraryIds = collections.stream()
                .filter(c -> c.getTargetType() == CollectionTargetType.ITINERARY)
                .map(Collection::getTargetId).distinct().toList();

        Map<Long, Guide> guideMap = new HashMap<>();
        if (!guideIds.isEmpty()) {
            for (Guide guide : guideMapper.selectBatchIds(guideIds)) {
                guideMap.put(guide.getId(), guide);
            }
        }

        Map<Long, Itinerary> itineraryMap = new HashMap<>();
        if (!itineraryIds.isEmpty()) {
            for (Itinerary itinerary : itineraryMapper.selectBatchIds(itineraryIds)) {
                itineraryMap.put(itinerary.getId(), itinerary);
            }
        }

        Set<Long> authorIds = guideMap.values().stream()
                .map(Guide::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> authorNames = new HashMap<>();
        if (!authorIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(authorIds)) {
                authorNames.put(user.getId(), user.getUsername());
            }
        }

        Set<Long> destinationIds = itineraryMap.values().stream()
                .map(Itinerary::getDestinationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> destinationNames = new HashMap<>();
        if (!destinationIds.isEmpty()) {
            for (Destination destination : destinationMapper.selectBatchIds(destinationIds)) {
                destinationNames.put(destination.getId(), destination.getName());
            }
        }

        List<Long> orphanCollectionIds = new ArrayList<>();
        List<CollectionItemResponse> items = new ArrayList<>();
        for (Collection collection : collections) {
            if (collection.getTargetType() == CollectionTargetType.GUIDE) {
                Guide guide = guideMap.get(collection.getTargetId());
                if (guide == null) {
                    orphanCollectionIds.add(collection.getId());
                    continue;
                }
                if (guide.getStatus() != GuideStatus.PUBLISHED) {
                    continue;
                }
                items.add(new CollectionItemResponse(
                        collection.getId(),
                        CollectionTargetType.GUIDE.name(),
                        guide.getId(),
                        guide.getTitle(),
                        guide.getCoverUrl(),
                        summarizeContent(guide.getContent()),
                        authorNames.get(guide.getAuthorId()),
                        collection.getCreatedAt()));
            } else if (collection.getTargetType() == CollectionTargetType.ITINERARY) {
                Itinerary itinerary = itineraryMap.get(collection.getTargetId());
                if (itinerary == null) {
                    orphanCollectionIds.add(collection.getId());
                    continue;
                }
                if (itinerary.getStatus() == ItineraryStatus.DRAFT
                        && !Objects.equals(itinerary.getUserId(), userId)) {
                    continue;
                }
                items.add(new CollectionItemResponse(
                        collection.getId(),
                        CollectionTargetType.ITINERARY.name(),
                        itinerary.getId(),
                        itinerary.getTitle(),
                        null,
                        buildItinerarySummary(itinerary, destinationNames),
                        null,
                        collection.getCreatedAt()));
            }
        }

        if (!orphanCollectionIds.isEmpty()) {
            collectionMapper.deleteBatchIds(orphanCollectionIds);
        }

        Page<CollectionItemResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(items);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCollected(Long userId, CollectionTargetType targetType, Long targetId) {
        return collectionMapper.selectCount(collectionWrapper(userId, targetType, targetId)) > 0;
    }

    @Override
    @Transactional
    public void collect(Long userId, CollectionTargetType targetType, Long targetId) {
        validateCollectible(userId, targetType, targetId);
        if (collectionMapper.selectCount(collectionWrapper(userId, targetType, targetId)) > 0) {
            return;
        }
        Collection collection = Collection.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        try {
            collectionMapper.insert(collection);
        } catch (DuplicateKeyException ignored) {
            // 并发重复收藏：唯一键兜底，视为成功
        }
    }

    @Override
    @Transactional
    public void uncollect(Long userId, CollectionTargetType targetType, Long targetId) {
        collectionMapper.delete(collectionWrapper(userId, targetType, targetId));
    }

    private LambdaQueryWrapper<Collection> collectionWrapper(Long userId,
                                                             CollectionTargetType targetType,
                                                             Long targetId) {
        return new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, userId)
                .eq(Collection::getTargetType, targetType)
                .eq(Collection::getTargetId, targetId);
    }

    private void validateCollectible(Long userId, CollectionTargetType targetType, Long targetId) {
        if (targetType == null) {
            throw new BusinessException("不支持的收藏类型");
        }
        switch (targetType) {
            case GUIDE -> {
                Guide guide = guideMapper.selectById(targetId);
                if (guide == null || guide.getStatus() != GuideStatus.PUBLISHED) {
                    throw new BusinessException("攻略不存在或未发布");
                }
            }
            case ITINERARY -> {
                Itinerary itinerary = itineraryMapper.selectById(targetId);
                if (itinerary == null) {
                    throw new ResourceNotFoundException("行程不存在");
                }
                if (itinerary.getStatus() == ItineraryStatus.DRAFT
                        && !Objects.equals(itinerary.getUserId(), userId)) {
                    throw new BusinessException("该行程未公开，无法收藏");
                }
            }
            default -> throw new BusinessException("不支持的收藏类型");
        }
    }

    private String summarizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content.replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= SUMMARY_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, SUMMARY_MAX_LENGTH);
    }

    private String buildItinerarySummary(Itinerary itinerary, Map<Long, String> destinationNames) {
        String dates = itinerary.getStartDate() + " ~ " + itinerary.getEndDate();
        String destinationName = itinerary.getDestinationId() == null
                ? null : destinationNames.get(itinerary.getDestinationId());
        if (destinationName == null || destinationName.isBlank()) {
            return dates;
        }
        return destinationName + " · " + dates;
    }
}
