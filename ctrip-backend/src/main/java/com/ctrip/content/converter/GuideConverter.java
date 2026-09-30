package com.ctrip.content.converter;

import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;

/**
 * 攻略 Entity ↔ DTO 转换器。
 */
public class GuideConverter {

    private GuideConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将创建请求转换为 Entity。
     *
     * @param authorId 作者用户 ID
     * @param request  创建请求
     */
    public static Guide toEntity(Long authorId, CreateGuideRequest request) {
        return Guide.builder()
                .authorId(authorId)
                .title(request.title())
                .content(request.content())
                .destinationId(request.destinationId())
                .coverUrl(request.coverUrl())
                .imageUrls(request.imageUrls())
                .status(request.status() != null ? request.status() : GuideStatus.DRAFT)
                .viewCount(0)
                .likeCount(0)
                .build();
    }

    /**
     * 将 Entity 转换为攻略详情响应 DTO。
     *
     * @param entity         攻略实体
     * @param destinationName 关联目的地名称（可为 null）
     * @param authorName      作者名字（可为 null）
     * @param authorAvatar    作者头像（可为 null）
     * @param commentCount    评论数量
     */
    public static GuideResponse toResponse(Guide entity, String destinationName, 
                                           String authorName, String authorAvatar, int commentCount) {
        return new GuideResponse(
                entity.getId(),
                entity.getAuthorId(),
                authorName,
                authorAvatar,
                entity.getTitle(),
                entity.getContent(),
                entity.getDestinationId(),
                destinationName,
                entity.getCoverUrl(),
                entity.getImageUrls(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getViewCount(),
                entity.getLikeCount(),
                commentCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 将 Entity 转换为攻略列表项响应 DTO。
     * 不包含 content 正文。
     *
     * @param entity         攻略实体
     * @param destinationName 关联目的地名称（可为 null）
     * @param authorName      作者名字（可为 null）
     * @param authorAvatar    作者头像（可为 null）
     * @param commentCount    评论数量
     */
    public static GuideListResponse toListResponse(Guide entity, String destinationName,
                                                    String authorName, String authorAvatar, int commentCount) {
        return new GuideListResponse(
                entity.getId(),
                entity.getAuthorId(),
                authorName,
                authorAvatar,
                entity.getTitle(),
                entity.getCoverUrl(),
                destinationName,
                entity.getViewCount(),
                entity.getLikeCount(),
                commentCount,
                entity.getCreatedAt()
        );
    }
}
