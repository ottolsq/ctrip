package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 编辑日程请求（部分更新）。
 */
public record UpdateItineraryDayRequest(
        @Size(max = 200) String title,
        Integer sortOrder
) {}
