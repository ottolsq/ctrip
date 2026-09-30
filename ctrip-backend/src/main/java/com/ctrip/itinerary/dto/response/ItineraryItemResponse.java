package com.ctrip.itinerary.dto.response;

/**
 * 行程项响应。
 */
public record ItineraryItemResponse(
        Long id,
        Long itineraryDayId,
        String type,
        String name,
        String location,
        String timeSlot,
        String description,
        Integer sortOrder
) {}
