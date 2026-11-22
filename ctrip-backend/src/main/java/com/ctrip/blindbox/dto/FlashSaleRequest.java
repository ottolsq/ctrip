package com.ctrip.blindbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 秒杀请求：模板 + 旅行偏好。
 */
public record FlashSaleRequest(
        @NotNull(message = "模板ID不能为空")
        Long templateId,

        @NotBlank(message = "出发城市不能为空")
        @Size(max = 20, message = "出发城市不能超过 20 字")
        String departureCity,

        @NotBlank(message = "预算等级不能为空")
        String budgetLevel,

        @Size(max = 50, message = "主题不能超过 50 字")
        String theme
) {
}
