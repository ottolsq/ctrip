package com.ctrip.content.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 目的地响应 DTO。
 * 包含关联的景点列表。
 */
public record DestinationResponse(
        Long id,
        String name,
        String country,
        String province,
        String description,
        String bestSeason,
        String coverUrl,
        String imageUrls,
        List<AttractionResponse> attractions,  // 关联景点列表
        LocalDateTime createdAt
) {}
