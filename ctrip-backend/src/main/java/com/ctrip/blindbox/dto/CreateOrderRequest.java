package com.ctrip.blindbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建盲盒订单请求。
 *
 * <p>用户在购买盲盒时提交的信息，包含模板ID和预选参数。
 * 预选参数（出发城市、预算等级、主题）将与订单在同一事务中保存到 blind_box_preference 表。
 */
public record CreateOrderRequest(

        /** 盲盒模板ID */
        @NotNull(message = "模板ID不能为空")
        Long templateId,

        /** 出发城市 */
        @NotBlank(message = "出发城市不能为空")
        String departureCity,

        /** 预算等级：ECONOMY / STANDARD / LUXURY */
        @NotNull(message = "预算等级不能为空")
        String budgetLevel,

        /** 旅行主题（可选） */
        String theme
) {
}
