package com.ctrip.itinerary.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行程详情响应（含完整嵌套日程和行程项）。
 */
public record ItineraryResponse(
        Long id,
        String title,
        Long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String shareCode,
        Integer viewCount,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ItineraryDayResponse> days
) {}
