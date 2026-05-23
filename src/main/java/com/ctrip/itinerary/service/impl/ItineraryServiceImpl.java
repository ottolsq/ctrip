package com.ctrip.itinerary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.itinerary.converter.ItineraryConverter;
import com.ctrip.itinerary.dto.request.*;
import com.ctrip.itinerary.dto.response.*;
import com.ctrip.itinerary.entity.*;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import com.ctrip.itinerary.entity.enums.ItemType;
import com.ctrip.itinerary.mapper.CollectionMapper;
import com.ctrip.itinerary.mapper.ItineraryDayMapper;
import com.ctrip.itinerary.mapper.ItineraryItemMapper;
import com.ctrip.itinerary.mapper.ItineraryMapper;
import com.ctrip.itinerary.service.ItineraryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * 行程服务实现类。
 */
@Service
public class ItineraryServiceImpl implements ItineraryService {

    private static final String BASE62_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHARE_CODE_LENGTH = 6;
    private static final long SHARE_EXPIRY_DAYS = 7;

    private final ItineraryMapper itineraryMapper;
    private final ItineraryDayMapper itineraryDayMapper;
    private final ItineraryItemMapper itineraryItemMapper;
    private final CollectionMapper collectionMapper;
    private final SecureRandom random = new SecureRandom();

    public ItineraryServiceImpl(ItineraryMapper itineraryMapper,
                                ItineraryDayMapper itineraryDayMapper,
                                ItineraryItemMapper itineraryItemMapper,
                                CollectionMapper collectionMapper) {
        this.itineraryMapper = itineraryMapper;
        this.itineraryDayMapper = itineraryDayMapper;
        this.itineraryItemMapper = itineraryItemMapper;
        this.collectionMapper = collectionMapper;
    }

    // ========== Itinerary CRUD ==========

    @Override
    @Transactional
    public ItineraryResponse createItinerary(Long userId, CreateItineraryRequest request) {
        validateDates(request.startDate(), request.endDate());

        Itinerary itinerary = ItineraryConverter.toEntity(request, userId);
        itineraryMapper.insert(itinerary);

        // 按起止日期自动生成日程
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        for (int i = 1; i <= days; i++) {
            ItineraryDay day = ItineraryDay.builder()
                    .itineraryId(itinerary.getId())
                    .dayNumber(i)
                    .title("Day " + i)
                    .sortOrder(i)
                    .build();
            itineraryDayMapper.insert(day);
        }

        return buildItineraryResponse(itinerary.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryListResponse> listItineraries(Long userId, int page, int limit,
                                                       String status, Long destinationId) {
        Page<Itinerary> pageObj = new Page<>(page, limit);

        LambdaQueryWrapper<Itinerary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Itinerary::getUserId, userId);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Itinerary::getStatus, ItineraryStatus.valueOf(status));
        }
        if (destinationId != null) {
            wrapper.eq(Itinerary::getDestinationId, destinationId);
        }
        wrapper.orderByDesc(Itinerary::getUpdatedAt);

        Page<Itinerary> result = itineraryMapper.selectPage(pageObj, wrapper);

        Page<ItineraryListResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(
                result.getRecords().stream()
                        .map(ItineraryConverter::toListResponse)
                        .toList()
        );
        return responsePage;
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getDetail(Long id, Long userId) {
        Itinerary itinerary = requireItinerary(id);

        // DRAFT 仅作者可见
        if (itinerary.getStatus() == ItineraryStatus.DRAFT && !itinerary.getUserId().equals(userId)) {
            throw new ForbiddenException("无权查看该行程");
        }

        // 非作者浏览量 +1
        if (!itinerary.getUserId().equals(userId)) {
            itineraryMapper.update(null, new LambdaUpdateWrapper<Itinerary>()
                    .eq(Itinerary::getId, id)
                    .setSql("view_count = view_count + 1"));
        }

        return buildItineraryResponse(id);
    }

    @Override
    @Transactional
    public ItineraryResponse updateItinerary(Long id, Long userId, UpdateItineraryRequest request) {
        Itinerary itinerary = requireItinerary(id);
        checkOwner(itinerary, userId);

        // 已归档的行程不允许修改
        if (itinerary.getStatus() == ItineraryStatus.ARCHIVED) {
            throw new ForbiddenException("已归档的行程不可修改");
        }

        LambdaUpdateWrapper<Itinerary> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Itinerary::getId, id);

        if (request.title() != null) {
            wrapper.set(Itinerary::getTitle, request.title());
        }
        if (request.destinationId() != null) {
            wrapper.set(Itinerary::getDestinationId, request.destinationId());
        }
        if (request.startDate() != null && request.endDate() != null) {
            validateDates(request.startDate(), request.endDate());
            wrapper.set(Itinerary::getStartDate, request.startDate())
                    .set(Itinerary::getEndDate, request.endDate());
        } else if (request.startDate() != null) {
            wrapper.set(Itinerary::getStartDate, request.startDate());
        } else if (request.endDate() != null) {
            wrapper.set(Itinerary::getEndDate, request.endDate());
        }
        if (request.status() != null) {
            wrapper.set(Itinerary::getStatus, ItineraryStatus.valueOf(request.status()));
        }

        itineraryMapper.update(null, wrapper);
        return buildItineraryResponse(id);
    }

    @Override
    @Transactional
    public void deleteItinerary(Long id, Long userId) {
        Itinerary itinerary = requireItinerary(id);
        checkOwner(itinerary, userId);

        // 级联删除：item → day → itinerary
        LambdaQueryWrapper<ItineraryDay> dayWrapper = new LambdaQueryWrapper<>();
        dayWrapper.eq(ItineraryDay::getItineraryId, id)
                .select(ItineraryDay::getId);
        List<ItineraryDay> days = itineraryDayMapper.selectList(dayWrapper);

        if (!days.isEmpty()) {
            List<Long> dayIds = days.stream().map(ItineraryDay::getId).toList();
            itineraryItemMapper.delete(new LambdaQueryWrapper<ItineraryItem>()
                    .in(ItineraryItem::getItineraryDayId, dayIds));
            itineraryDayMapper.delete(new LambdaQueryWrapper<ItineraryDay>()
                    .in(ItineraryDay::getId, dayIds));
        }

        itineraryMapper.deleteById(id);
    }

    // ========== Collection ==========

    @Override
    @Transactional
    public void toggleCollection(Long id, Long userId, boolean add) {
        requireItineraryExists(id);

        if (add) {
            // 防止重复收藏
            LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Collection::getUserId, userId)
                    .eq(Collection::getTargetType, CollectionTargetType.ITINERARY)
                    .eq(Collection::getTargetId, id);
            if (collectionMapper.selectCount(wrapper) == 0) {
                Collection collection = Collection.builder()
                        .userId(userId)
                        .targetType(CollectionTargetType.ITINERARY)
                        .targetId(id)
                        .build();
                collectionMapper.insert(collection);
            }
        } else {
            collectionMapper.delete(new LambdaQueryWrapper<Collection>()
                    .eq(Collection::getUserId, userId)
                    .eq(Collection::getTargetType, CollectionTargetType.ITINERARY)
                    .eq(Collection::getTargetId, id));
        }
    }

    // ========== Days ==========

    @Override
    @Transactional
    public ItineraryResponse addDay(Long itineraryId, Long userId, CreateItineraryDayRequest request) {
        Itinerary itinerary = requireItinerary(itineraryId);
        checkOwner(itinerary, userId);

        // 计算当前最大 sortOrder
        LambdaQueryWrapper<ItineraryDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItineraryDay::getItineraryId, itineraryId)
                .orderByDesc(ItineraryDay::getSortOrder)
                .last("LIMIT 1");
        ItineraryDay lastDay = itineraryDayMapper.selectOne(wrapper);
        int sortOrder = (lastDay != null ? lastDay.getSortOrder() : 0) + 1;

        ItineraryDay day = ItineraryConverter.toDayEntity(request, itineraryId, sortOrder);
        itineraryDayMapper.insert(day);

        return buildItineraryResponse(itineraryId);
    }

    @Override
    @Transactional
    public ItineraryDayResponse updateDay(Long dayId, Long userId, UpdateItineraryDayRequest request) {
        ItineraryDay day = requireDayOwnership(dayId, userId);

        LambdaUpdateWrapper<ItineraryDay> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ItineraryDay::getId, dayId);

        if (request.title() != null) {
            wrapper.set(ItineraryDay::getTitle, request.title());
        }
        if (request.sortOrder() != null) {
            wrapper.set(ItineraryDay::getSortOrder, request.sortOrder());
        }

        itineraryDayMapper.update(null, wrapper);
        return buildDayResponse(dayId);
    }

    @Override
    @Transactional
    public void deleteDay(Long dayId, Long userId) {
        ItineraryDay day = requireDayOwnership(dayId, userId);

        // 级联删除该日程下的所有 item
        itineraryItemMapper.delete(new LambdaQueryWrapper<ItineraryItem>()
                .eq(ItineraryItem::getItineraryDayId, dayId));
        itineraryDayMapper.deleteById(dayId);
    }

    // ========== Items ==========

    @Override
    @Transactional
    public ItineraryItemResponse addItem(Long itineraryId, Long dayId, Long userId,
                                         CreateItineraryItemRequest request) {
        // 校验行程所有权
        Itinerary itinerary = requireItinerary(itineraryId);
        checkOwner(itinerary, userId);

        // 校验日程属于该行程
        ItineraryDay day = itineraryDayMapper.selectById(dayId);
        if (day == null || !day.getItineraryId().equals(itineraryId)) {
            throw new ResourceNotFoundException("日程不存在或不属于该行程");
        }

        // 计算当前最大 sortOrder
        LambdaQueryWrapper<ItineraryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItineraryItem::getItineraryDayId, dayId)
                .orderByDesc(ItineraryItem::getSortOrder)
                .last("LIMIT 1");
        ItineraryItem lastItem = itineraryItemMapper.selectOne(wrapper);
        int sortOrder = (lastItem != null ? lastItem.getSortOrder() : 0) + 1;

        ItineraryItem item = ItineraryConverter.toItemEntity(request, dayId, sortOrder);
        itineraryItemMapper.insert(item);

        return ItineraryConverter.toItemResponse(item);
    }

    @Override
    @Transactional
    public ItineraryItemResponse updateItem(Long itemId, Long userId, UpdateItineraryItemRequest request) {
        ItineraryItem item = requireItemOwnership(itemId, userId);

        LambdaUpdateWrapper<ItineraryItem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ItineraryItem::getId, itemId);

        if (request.type() != null) {
            wrapper.set(ItineraryItem::getType, request.type());
        }
        if (request.name() != null) {
            wrapper.set(ItineraryItem::getName, request.name());
        }
        if (request.location() != null) {
            wrapper.set(ItineraryItem::getLocation, request.location());
        }
        if (request.timeSlot() != null) {
            wrapper.set(ItineraryItem::getTimeSlot, request.timeSlot());
        }
        if (request.description() != null) {
            wrapper.set(ItineraryItem::getDescription, request.description());
        }
        if (request.sortOrder() != null) {
            wrapper.set(ItineraryItem::getSortOrder, request.sortOrder());
        }

        itineraryItemMapper.update(null, wrapper);
        return ItineraryConverter.toItemResponse(requireItem(itemId));
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        ItineraryItem item = requireItemOwnership(itemId, userId);
        itineraryItemMapper.deleteById(itemId);
    }

    @Override
    @Transactional
    public void reorderItems(Long dayId, Long userId, ReorderItemsRequest request) {
        requireDayOwnership(dayId, userId);

        // 按传入顺序更新 sortOrder
        for (int i = 0; i < request.itemIds().size(); i++) {
            itineraryItemMapper.update(null, new LambdaUpdateWrapper<ItineraryItem>()
                    .eq(ItineraryItem::getId, request.itemIds().get(i))
                    .eq(ItineraryItem::getItineraryDayId, dayId)
                    .set(ItineraryItem::getSortOrder, i + 1));
        }
    }

    // ========== Share ==========

    @Override
    @Transactional
    public String generateShareLink(Long id, Long userId) {
        Itinerary itinerary = requireItinerary(id);
        checkOwner(itinerary, userId);

        String code = generateShareCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(SHARE_EXPIRY_DAYS);

        itineraryMapper.update(null, new LambdaUpdateWrapper<Itinerary>()
                .eq(Itinerary::getId, id)
                .set(Itinerary::getShareCode, code)
                .set(Itinerary::getShareExpiresAt, expiresAt));

        return code;
    }

    @Override
    @Transactional
    public ItineraryResponse getSharedItinerary(String shareCode) {
        LambdaQueryWrapper<Itinerary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Itinerary::getShareCode, shareCode);
        Itinerary itinerary = itineraryMapper.selectOne(wrapper);

        if (itinerary == null) {
            throw new ResourceNotFoundException("分享链接无效");
        }

        // 检查过期
        if (itinerary.getShareExpiresAt() != null
                && itinerary.getShareExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("分享链接已过期");
        }

        // 浏览量 +1
        itineraryMapper.update(null, new LambdaUpdateWrapper<Itinerary>()
                .eq(Itinerary::getId, itinerary.getId())
                .setSql("view_count = view_count + 1"));

        return buildItineraryResponse(itinerary.getId());
    }

    @Override
    @Transactional
    public void cancelShare(Long id, Long userId) {
        Itinerary itinerary = requireItinerary(id);
        checkOwner(itinerary, userId);

        itineraryMapper.update(null, new LambdaUpdateWrapper<Itinerary>()
                .eq(Itinerary::getId, id)
                .set(Itinerary::getShareCode, null)
                .set(Itinerary::getShareExpiresAt, null));
    }

    // ========== 私有辅助 ==========

    private Itinerary requireItinerary(Long id) {
        Itinerary itinerary = itineraryMapper.selectById(id);
        if (itinerary == null) {
            throw new ResourceNotFoundException("行程不存在：id=" + id);
        }
        return itinerary;
    }

    private void requireItineraryExists(Long id) {
        if (itineraryMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("行程不存在：id=" + id);
        }
    }

    private void checkOwner(Itinerary itinerary, Long userId) {
        if (!itinerary.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作他人行程");
        }
    }

    private ItineraryDay requireDay(Long id) {
        ItineraryDay day = itineraryDayMapper.selectById(id);
        if (day == null) {
            throw new ResourceNotFoundException("日程不存在：id=" + id);
        }
        return day;
    }

    private ItineraryDay requireDayOwnership(Long dayId, Long userId) {
        ItineraryDay day = requireDay(dayId);
        Itinerary itinerary = requireItinerary(day.getItineraryId());
        checkOwner(itinerary, userId);
        return day;
    }

    private ItineraryItem requireItem(Long id) {
        ItineraryItem item = itineraryItemMapper.selectById(id);
        if (item == null) {
            throw new ResourceNotFoundException("行程项不存在：id=" + id);
        }
        return item;
    }

    private ItineraryItem requireItemOwnership(Long itemId, Long userId) {
        ItineraryItem item = requireItem(itemId);
        ItineraryDay day = requireDay(item.getItineraryDayId());
        Itinerary itinerary = requireItinerary(day.getItineraryId());
        checkOwner(itinerary, userId);
        return item;
    }

    private ItineraryResponse buildItineraryResponse(Long itineraryId) {
        Itinerary itinerary = requireItinerary(itineraryId);

        List<ItineraryDay> days = itineraryDayMapper.selectList(
                new LambdaQueryWrapper<ItineraryDay>()
                        .eq(ItineraryDay::getItineraryId, itineraryId)
                        .orderByAsc(ItineraryDay::getSortOrder)
        );

        List<Long> dayIds = days.stream().map(ItineraryDay::getId).toList();
        List<ItineraryItem> items = dayIds.isEmpty() ? List.of()
                : itineraryItemMapper.selectList(
                new LambdaQueryWrapper<ItineraryItem>()
                        .in(ItineraryItem::getItineraryDayId, dayIds)
                        .orderByAsc(ItineraryItem::getSortOrder)
        );

        return ItineraryConverter.toResponse(itinerary, days, items);
    }

    private ItineraryDayResponse buildDayResponse(Long dayId) {
        ItineraryDay day = requireDay(dayId);
        List<ItineraryItemResponse> itemResponses = itineraryItemMapper.selectList(
                new LambdaQueryWrapper<ItineraryItem>()
                        .eq(ItineraryItem::getItineraryDayId, dayId)
                        .orderByAsc(ItineraryItem::getSortOrder)
        ).stream().map(ItineraryConverter::toItemResponse).toList();
        return ItineraryConverter.toDayResponse(day, itemResponses);
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private String generateShareCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(SHARE_CODE_LENGTH);
            for (int j = 0; j < SHARE_CODE_LENGTH; j++) {
                sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
            }
            String code = sb.toString();
            LambdaQueryWrapper<Itinerary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Itinerary::getShareCode, code);
            if (itineraryMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        throw new RuntimeException("生成分享码失败，请重试");
    }
}
