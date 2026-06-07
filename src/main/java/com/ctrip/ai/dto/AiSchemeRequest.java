package com.ctrip.ai.dto;

/**
 * AI 旅行方案生成请求 DTO。
 *
 * <p>业务模块将自身参数转换为此 DTO，调用 AI 模块生成旅行方案。
 */
public record AiSchemeRequest(
        /** 出发城市（如 "北京"、"上海"） */
        String departureCity,

        /** 预算等级：ECONOMY / STANDARD / LUXURY */
        String budgetLevel,

        /** 旅行主题（如 "美食"、"海滨"、"古镇"，可为空） */
        String theme,

        /** 期望行程天数（可选，为 null 时由 AI 根据预算等级决定） */
        Integer preferredDays,

        /** 其他偏好 JSON（可选扩展字段，如 "不爬山"、"带老人" 等） */
        String preferences
) {
}
