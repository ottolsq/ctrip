package com.ctrip.blindbox.dto;

import java.time.LocalDateTime;

/**
 * 抽取记录响应。
 */
public record DrawRecordResponse(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String userName,
        Long templateId,
        String templateName,
        String tier,
        Long destinationId,
        String destination,
        boolean guaranteed,
        String probabilityVersion,
        LocalDateTime createdAt
) {
}
