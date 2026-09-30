package com.ctrip.ai.prompt;

import com.ctrip.ai.dto.AiSchemeRequest;
import org.springframework.stereotype.Component;

/**
 * AI User Prompt 构建器。
 *
 * <p>根据业务模块传入的 {@link AiSchemeRequest}，
 * 动态生成结构化的 User Prompt 文本。
 */
@Component
public class PromptBuilder {

    /**
     * 根据请求参数构建 User Prompt。
     *
     * @param request 用户偏好信息
     * @return 格式化后的 User Prompt 文本
     */
    public String build(AiSchemeRequest request) {
        // 预算等级对应的金额范围描述
        String budgetDesc = switch (request.budgetLevel()) {
            case "ECONOMY" -> "经济型（总预算约 1500-2000 元，适合穷游/学生党）";
            case "STANDARD" -> "标准型（总预算约 2500-3500 元，舒适旅行）";
            case "LUXURY" -> "豪华型（总预算约 5000-8000 元，高品质度假体验）";
            default -> "标准型（总预算约 2500-3500 元，舒适旅行）";
        };

        // 主题偏好提示
        String themeHint = (request.theme() != null && !request.theme().isBlank())
                ? "用户偏好的主题是「" + request.theme() + "」。请在行程中充分体现这一主题。"
                : "用户没有指定主题，请随机选择一个适合该目的地的主题（如美食、文化、海滨等）。";

        // 天数决定策略
        String daysHint = (request.preferredDays() != null)
                ? "用户期望行程天数为 " + request.preferredDays() + " 天。"
                : "请根据预算等级自动决定天数（ECONOMY=3天，STANDARD=4天，LUXURY=5天）。";

        return String.format("""
                请为以下用户生成旅行方案：

                - 出发城市：%s
                - 预算等级：%s - %s
                - 主题偏好：%s
                - 行程天数：%s

                请从 %s 出发，规划到目的地的合理交通方案。
                请严格按照 JSON 格式返回完整方案。
                """,
                request.departureCity(),
                request.budgetLevel(),
                budgetDesc,
                themeHint,
                daysHint,
                request.departureCity());
    }
}
