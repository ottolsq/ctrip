package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 更新行程请求（部分更新，所有字段可选）。
 */
public record UpdateItineraryRequest(
        @Size(max = 200) String title,
        Long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {}
