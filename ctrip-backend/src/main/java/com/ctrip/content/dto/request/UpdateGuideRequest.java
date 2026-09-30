package com.ctrip.content.dto.request;

import com.ctrip.content.entity.enums.GuideStatus;
import jakarta.validation.constraints.Size;

/**
 * 更新攻略请求 DTO。
 * 所有字段均为可选，仅传入需要修改的字段。
 */
public record UpdateGuideRequest(

        @Size(max = 200)
        String title,

        String content,

        Long destinationId,

        @Size(max = 500)
        String coverUrl,

        String imageUrls,

        GuideStatus status

) {}
