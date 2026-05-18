package com.ctrip.content.converter;

import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.DestinationResponse;
import com.ctrip.content.entity.Destination;

/**
 * 目的地 Entity ↔ DTO 转换器。
 *
 * <p>设计约定：仅包含静态方法，无状态，不注册为 Spring Bean。
 */
public class DestinationConverter {

    private DestinationConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将创建请求转换为 Entity。
     */
    public static Destination toEntity(CreateDestinationRequest request) {
        return Destination.builder()
                .name(request.name())
                .country(request.country())
                .province(request.province())
                .description(request.description())
                .bestSeason(request.bestSeason())
                .coverUrl(request.coverUrl())
                .imageUrls(request.imageUrls())
                .build();
    }

    /**
     * 将 Entity 转换为响应 DTO。
     * 关联的景点列表由调用方单独传入。
     */
    public static DestinationResponse toResponse(Destination entity) {
        return new DestinationResponse(
                entity.getId(),
                entity.getName(),
                entity.getCountry(),
                entity.getProvince(),
                entity.getDescription(),
                entity.getBestSeason() != null ? entity.getBestSeason().name() : null,
                entity.getCoverUrl(),
                entity.getImageUrls(),
                null,  // 景点列表由 Service 层单独填充
                entity.getCreatedAt()
        );
    }

    /**
     * 将 Entity 转换为带景点列表的响应 DTO。
     */
    public static DestinationResponse toResponse(Destination entity, java.util.List<com.ctrip.content.dto.response.AttractionResponse> attractions) {
        return new DestinationResponse(
                entity.getId(),
                entity.getName(),
                entity.getCountry(),
                entity.getProvince(),
                entity.getDescription(),
                entity.getBestSeason() != null ? entity.getBestSeason().name() : null,
                entity.getCoverUrl(),
                entity.getImageUrls(),
                attractions,
                entity.getCreatedAt()
        );
    }
}
