package com.ctrip.blindbox.dto;

import java.util.Map;

/**
 * 盲盒方案响应。
 *
 * <p>MVP 阶段由规则引擎生成 mock 方案，
 * 后续接入 AI API 后替换为真实生成逻辑。
 * 方案结构：目的地 + 行程数据 + 酒店 + 交通 + 预算。
 */
public record SchemeResponse(
        String destination,
        Long destinationId,
        String theme,
        String budgetLevel,
        Integer days,
        Map<String, Object> itinerary,
        Map<String, Object> hotel,
        Map<String, Object> transport,
        Map<String, Object> budget
) {
}
