package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ctrip.itinerary.entity.enums.ItemType;

/**
 * 添加行程项请求。
 */
public record CreateItineraryItemRequest(
        @NotNull(message = "行程项类型不能为空")
        ItemType type,

        @NotBlank(message = "名称不能为空")
        @Size(max = 200)
        String name,

        @Size(max = 300)
        String location,

        @Size(max = 50)
        String timeSlot,

        String description,

        Integer sortOrder
) {}
