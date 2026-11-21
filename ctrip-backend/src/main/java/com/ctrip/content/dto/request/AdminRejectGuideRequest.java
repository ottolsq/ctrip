package com.ctrip.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端驳回攻略请求。
 */
public record AdminRejectGuideRequest(
        @NotBlank(message = "驳回理由不能为空")
        @Size(max = 500)
        String reason
) {}
