package com.ctrip.content.dto.request;

import com.ctrip.content.entity.enums.Season;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建目的地请求 DTO（管理端）。
 */
public record CreateDestinationRequest(

        /** 目的地名称 */
        @NotBlank(message = "目的地名称不能为空")
        @Size(max = 100)
        String name,

        /** 所属国家 */
        @NotBlank(message = "国家不能为空")
        @Size(max = 60)
        String country,

        /** 所属省份/州 */
        @Size(max = 60)
        String province,

        /** 目的地简介 */
        String description,

        /** 最佳旅游季节 */
        Season bestSeason,

        /** 封面图 URL */
        @Size(max = 500)
        String coverUrl,

        /** 多图 JSON 数组 */
        String imageUrls

) {}
