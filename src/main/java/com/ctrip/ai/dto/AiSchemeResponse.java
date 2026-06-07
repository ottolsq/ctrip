package com.ctrip.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI 原始 JSON 响应 DTO（内部使用）。
 *
 * <p>用于反序列化 DashScope AI 返回的 JSON 文本，
 * 之后转换为标准化的 {@link SchemeOutput} 供业务模块使用。
 */
public record AiSchemeResponse(
        /** 目的地城市名称 */
        String destination,

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
            /** 当日主题标题（如 "抵达成都"、"文化体验日"） */
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
            /** 交通方式（飞机/高铁/...） */
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
