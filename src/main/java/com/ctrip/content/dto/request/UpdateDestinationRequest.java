package com.ctrip.content.dto.request;

import com.ctrip.content.entity.enums.Season;
import jakarta.validation.constraints.Size;

/**
 * 更新目的地请求 DTO（管理端）。
 * 所有字段均为可选，仅传入需要修改的字段。
 */
public record UpdateDestinationRequest(

        @Size(max = 100)
        String name,

        @Size(max = 60)
        String country,

        @Size(max = 60)
        String province,

        String description,

        Season bestSeason,

        @Size(max = 500)
        String coverUrl,

        String imageUrls

) {}
