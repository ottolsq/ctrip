package com.ctrip.itinerary.converter;

import com.ctrip.itinerary.dto.request.CreateItineraryDayRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryItemRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryRequest;
import com.ctrip.itinerary.dto.response.ItineraryDayResponse;
import com.ctrip.itinerary.dto.response.ItineraryItemResponse;
import com.ctrip.itinerary.dto.response.ItineraryListResponse;
import com.ctrip.itinerary.dto.response.ItineraryResponse;
import com.ctrip.itinerary.entity.Itinerary;
import com.ctrip.itinerary.entity.ItineraryDay;
import com.ctrip.itinerary.entity.ItineraryItem;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行程模块 Entity ↔ DTO 转换器。
 *
 * 无状态工具类，不注册为 Spring Bean。
 * 枚举统一通过 .name() 转为 String。
 */
public class ItineraryConverter {

    private ItineraryConverter() {
    }

    // ========== Entity → Response ==========

    public static ItineraryListResponse toListResponse(Itinerary entity) {
        return new ItineraryListResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDestinationId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getViewCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ItineraryResponse toResponse(Itinerary entity, List<ItineraryDay> days, List<ItineraryItem> items) {
        java.util.Map<Long, List<ItineraryItem>> itemsByDay = items.stream()
                .collect(Collectors.groupingBy(ItineraryItem::getItineraryDayId));

        List<ItineraryDayResponse> dayResponses = days.stream()
                .map(day -> {
                    List<ItineraryItem> dayItems = itemsByDay.getOrDefault(day.getId(), List.of());
                    List<ItineraryItemResponse> itemResponses = dayItems.stream()
                            .map(ItineraryConverter::toItemResponse)
                            .collect(Collectors.toList());
                    return toDayResponse(day, itemResponses);
                })
                .collect(Collectors.toList());

        return new ItineraryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDestinationId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getShareCode(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                dayResponses
        );
    }

    public static ItineraryDayResponse toDayResponse(ItineraryDay day, List<ItineraryItemResponse> items) {
        return new ItineraryDayResponse(
                day.getId(),
                day.getDayNumber(),
                day.getTitle(),
                day.getSortOrder(),
                items
        );
    }

    public static ItineraryItemResponse toItemResponse(ItineraryItem item) {
        return new ItineraryItemResponse(
                item.getId(),
                item.getItineraryDayId(),
                item.getType() != null ? item.getType().name() : null,
                item.getName(),
                item.getLocation(),
                item.getTimeSlot(),
                item.getDescription(),
                item.getSortOrder()
        );
    }

    // ========== Request → Entity ==========

    public static Itinerary toEntity(CreateItineraryRequest request, Long userId) {
        return Itinerary.builder()
                .userId(userId)
                .title(request.title())
                .destinationId(request.destinationId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(ItineraryStatus.DRAFT)
                .viewCount(0)
                .likeCount(0)
                .build();
    }

    public static ItineraryDay toDayEntity(CreateItineraryDayRequest request, Long itineraryId, int sortOrder) {
        return ItineraryDay.builder()
                .itineraryId(itineraryId)
                .dayNumber(request.dayNumber())
                .title(request.title())
                .sortOrder(sortOrder)
                .build();
    }

    public static ItineraryItem toItemEntity(CreateItineraryItemRequest request, Long dayId, int sortOrder) {
        return ItineraryItem.builder()
                .itineraryDayId(dayId)
                .type(request.type())
                .name(request.name())
                .location(request.location())
                .timeSlot(request.timeSlot())
                .description(request.description())
                .sortOrder(sortOrder)
                .build();
    }
}
