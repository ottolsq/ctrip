package com.ctrip.itinerary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.itinerary.dto.response.CollectionItemResponse;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;

/**
 * 通用收藏服务（攻略/行程）。
 */
public interface CollectionService {

    /**
     * 分页查询当前用户的收藏。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页数量
     * @param type   目标类型；为 null 时查询攻略与行程
     */
    Page<CollectionItemResponse> listCollections(Long userId, int page, int size, CollectionTargetType type);

    /** 判断用户是否已收藏目标。 */
    boolean isCollected(Long userId, CollectionTargetType targetType, Long targetId);

    /** 收藏目标；重复收藏幂等。 */
    void collect(Long userId, CollectionTargetType targetType, Long targetId);

    /** 取消收藏；目标不存在时静默成功。 */
    void uncollect(Long userId, CollectionTargetType targetType, Long targetId);
}
