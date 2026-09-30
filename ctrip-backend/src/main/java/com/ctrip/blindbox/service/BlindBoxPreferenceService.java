package com.ctrip.blindbox.service;

import com.ctrip.blindbox.entity.BlindBoxPreference;

/**
 * 盲盒预选参数服务接口。
 *
 * 负责偏好的保存与查询，通常与订单创建在同一事务中完成。
 */
public interface BlindBoxPreferenceService {

    /**
     * 保存预选参数（与订单创建在同一事务中）。
     *
     * @param orderId      关联的订单ID
     * @param departureCity 出发城市
     * @param budgetLevel   预算等级字符串（ECONOMY/STANDARD/LUXURY）
     * @param theme         旅行主题（可选）
     * @return 保存的偏好实体
     */
    BlindBoxPreference savePreference(Long orderId, String departureCity, String budgetLevel, String theme);

    /**
     * 通过订单ID查询预选参数。
     *
     * @param orderId 订单ID
     * @return 偏好实体，不存在时返回 null
     */
    BlindBoxPreference getByOrderId(Long orderId);
}
