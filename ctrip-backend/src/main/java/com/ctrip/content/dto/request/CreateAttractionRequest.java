package com.ctrip.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建景点请求 DTO（管理端）。
 */
public record CreateAttractionRequest(

        /** 所属目的地 ID */
        @NotNull(message = "目的地 ID 不能为空")
        Long destinationId,

        /** 景点名称 */
        @NotBlank(message = "景点名称不能为空")
        @Size(max = 200)
        String name,

        /** 景点简介 */
        String description,

        /** 详细地址/坐标 */
        @Size(max = 300)
        String location,

        /** 门票价格，NULL 表示免费 */
        BigDecimal ticketPrice,

        /** 封面图 URL */
        @Size(max = 500)
        String coverUrl,

        /** 多图 JSON 数组 */
        String imageUrls

) {}
