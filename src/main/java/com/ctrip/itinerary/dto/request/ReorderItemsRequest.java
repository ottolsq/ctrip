package com.ctrip.itinerary.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量排序行程项请求。
 */
public record ReorderItemsRequest(
        @NotEmpty(message = "行程项 ID 列表不能为空")
        List<Long> itemIds
) {}
