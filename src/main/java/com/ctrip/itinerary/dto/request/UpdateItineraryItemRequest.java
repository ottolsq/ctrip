package com.ctrip.itinerary.dto.request;

import com.ctrip.itinerary.entity.enums.ItemType;
import jakarta.validation.constraints.Size;

/**
 * 编辑行程项请求（部分更新）。
 */
public record UpdateItineraryItemRequest(
        ItemType type,

        @Size(max = 200) String name,

        @Size(max = 300) String location,

        @Size(max = 50) String timeSlot,

        String description,

        Integer sortOrder
) {}
