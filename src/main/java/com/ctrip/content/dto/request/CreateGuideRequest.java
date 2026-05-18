package com.ctrip.content.dto.request;

import com.ctrip.content.entity.enums.GuideStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建攻略请求 DTO。
 */
public record CreateGuideRequest(

        /** 攻略标题 */
        @NotBlank(message = "攻略标题不能为空")
        @Size(max = 200)
        String title,

        /** 攻略正文（Markdown 格式） */
        @NotBlank(message = "攻略内容不能为空")
        String content,

        /** 关联目的地 ID（可为 NULL，纯经验贴） */
        Long destinationId,

        /** 封面图 URL */
        @Size(max = 500)
        String coverUrl,

        /** 多图 JSON 数组 */
        String imageUrls,

        /** 发布状态：草稿或已发布 */
        GuideStatus status

) {}
