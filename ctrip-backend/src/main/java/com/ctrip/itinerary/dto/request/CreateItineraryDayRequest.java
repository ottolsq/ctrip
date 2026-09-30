package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 添加日程请求。
 */
public record CreateItineraryDayRequest(
        @NotNull(message = "日程序号不能为空")
        Integer dayNumber,

        @NotBlank(message = "日程标题不能为空")
        String title
) {}
