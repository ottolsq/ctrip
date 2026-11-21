package com.ctrip.operation.dto;

import com.ctrip.operation.entity.enums.DisplayStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 轮播图创建/更新请求。
 */
public record BannerRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过 100 字")
        String title,

        @NotBlank(message = "图片地址不能为空")
        @Size(max = 500, message = "图片地址不能超过 500 字")
        String imageUrl,

        @Size(max = 500, message = "跳转地址不能超过 500 字")
        String linkUrl,

        Integer sortOrder,

        DisplayStatus status,

        LocalDateTime startAt,

        LocalDateTime endAt
) {
}
