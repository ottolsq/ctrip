package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 创建行程请求。
 */
public record CreateItineraryRequest(
        @NotBlank(message = "行程标题不能为空")
        @Size(max = 200)
        String title,

        Long destinationId,

        @NotNull(message = "开始日期不能为空")
        LocalDate startDate,

        @NotNull(message = "结束日期不能为空")
        LocalDate endDate
) {}
