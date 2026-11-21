package com.ctrip.operation.dto;

import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.entity.enums.RecommendationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 推荐位创建/更新请求。
 */
public record RecommendationRequest(
        @NotBlank(message = "位置标识不能为空")
        @Size(max = 50, message = "位置标识不能超过 50 字")
        String slot,

        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过 100 字")
        String title,

        @NotNull(message = "目标类型不能为空")
        RecommendationTargetType targetType,

        @NotNull(message = "目标ID不能为空")
        Long targetId,

        Integer sortOrder,

        DisplayStatus status,

        LocalDateTime startAt,

        LocalDateTime endAt
) {
}
