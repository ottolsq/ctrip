package com.ctrip.blindbox.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盲盒模板响应。
 *
 * <p>返回给前端的模板信息，区分日常/限定类型。
 * 限定盲盒显示库存数量，日常盲盒 stock 显示 -1。
 */
public record TemplateResponse(
        Long id,
        String name,
        String type,
        BigDecimal price,
        Integer stock,
        String ruleConfig,
        String status,
        LocalDateTime createdAt
) {
}
