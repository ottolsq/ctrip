package com.ctrip.itinerary.dto.response;

import java.util.List;

/**
 * 日程响应（含嵌套行程项）。
 */
public record ItineraryDayResponse(
        Long id,
        Integer dayNumber,
        String title,
        Integer sortOrder,
        List<ItineraryItemResponse> items
) {}
