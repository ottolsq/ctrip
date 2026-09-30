package com.ctrip.content.dto.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新景点请求 DTO（管理端）。
 * 所有字段均为可选，仅传入需要修改的字段。
 */
public record UpdateAttractionRequest(

        @Size(max = 200)
        String name,

        String description,

        @Size(max = 300)
        String location,

        BigDecimal ticketPrice,

        @Size(max = 500)
        String coverUrl,

        String imageUrls

) {}
