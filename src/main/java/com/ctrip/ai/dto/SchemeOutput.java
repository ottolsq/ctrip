package com.ctrip.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI 生成的旅行方案标准化输出 DTO。
 *
 * <p>业务模块接收此 DTO，自行决定如何使用（序列化入库、返回前端等）。
 * 与具体 AI 平台解耦，便于未来切换模型供应商。
 */
public record SchemeOutput(
        /** 目的地城市名称 */
        String destination,

        /** 目的地ID（可选，由调用方决定是否关联 destination 表） */
        Long destinationId,

        /** 旅行主题 */
        String theme,

        /** 行程天数 */
        Integer days,

        /** 每日行程，key 为 "day1", "day2" 等 */
        Map<String, DayPlan> itinerary,

        /** 酒店推荐 */
        HotelInfo hotel,

        /** 交通方案 */
        TransportInfo transport,

        /** 预算明细 */
        BudgetInfo budget
) {
    /** 单日行程计划。 */
    public record DayPlan(
            /** 当日主题标题 */
            String title,
            /** 具体活动列表 */
            List<String> activities
    ) {}

    /** 酒店信息。 */
    public record HotelInfo(
            /** 酒店名称 */
            String name,
            /** 酒店地址/区域 */
            String address,
            /** 评分（1-5） */
            Double rating
    ) {}

    /** 交通信息。 */
    public record TransportInfo(
            /** 交通方式 */
            String type,
            /** 建议出发时间 */
            String departureTime,
            /** 交通方案详细描述 */
            String description
    ) {}

    /** 预算明细。 */
    public record BudgetInfo(
            /** 总预算（CNY） */
            Integer total,
            /** 交通费用 */
            Integer transport,
            /** 酒店费用 */
            Integer hotel,
            /** 餐饮费用 */
            Integer food,
            /** 门票/活动费用 */
            Integer tickets
    ) {}
}
