package com.ctrip.itinerary.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 行程列表响应（不含嵌套日程/行程项，避免 N+1）。
 */
public record ItineraryListResponse(
        Long id,
        String title,
        Long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
