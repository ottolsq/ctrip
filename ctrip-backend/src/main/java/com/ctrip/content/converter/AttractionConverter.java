package com.ctrip.content.converter;

import com.ctrip.content.dto.request.CreateAttractionRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.entity.Attraction;

/**
 * 景点 Entity ↔ DTO 转换器。
 */
public class AttractionConverter {

    private AttractionConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将创建请求转换为 Entity。
     */
    public static Attraction toEntity(CreateAttractionRequest request) {
        return Attraction.builder()
                .destinationId(request.destinationId())
                .name(request.name())
                .description(request.description())
                .location(request.location())
                .ticketPrice(request.ticketPrice())
                .coverUrl(request.coverUrl())
                .imageUrls(request.imageUrls())
                .build();
    }

    /**
     * 将 Entity 转换为响应 DTO。
     */
    public static AttractionResponse toResponse(Attraction entity) {
        return new AttractionResponse(
                entity.getId(),
                entity.getDestinationId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getTicketPrice(),
                entity.getCoverUrl(),
                entity.getImageUrls(),
                entity.getCreatedAt()
        );
    }
}
