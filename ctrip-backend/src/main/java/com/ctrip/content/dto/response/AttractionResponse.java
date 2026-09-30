package com.ctrip.content.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点响应 DTO。
 */
public record AttractionResponse(
        Long id,
        Long destinationId,
        String name,
        String description,
        String location,
        BigDecimal ticketPrice,
        String coverUrl,
        String imageUrls,
        LocalDateTime createdAt
) {}
