package com.ctrip.blindbox.dto;

import java.time.LocalDateTime;

/**
 * 秒杀订单异步消息 DTO。
 *
 * <p>秒杀流程中，Lua 脚本原子扣减 Redis 库存后，通过 RabbitMQ 将此消息发送给
 * {@code FlashSaleOrderConsumer} 异步创建订单。
 *
 * <p>消息体保持最小——仅包含创建订单所需的必要字段，避免传输冗余数据。
 *
 * @param userId     用户 ID
 * @param templateId 盲盒模板 ID
 * @param orderNo    预生成的订单号（格式：BB + yyyyMMdd + 4 位序号）
 * @param timestamp  秒杀请求时间戳（用于监控消息延迟）
 */
public record FlashSaleMessage(
        Long userId,
        Long templateId,
        String orderNo,
        LocalDateTime timestamp,
        String departureCity,
        String budgetLevel,
        String theme
) {
    /**
     * 工厂方法：创建带旅行偏好的秒杀消息。
     */
    public static FlashSaleMessage of(Long userId, Long templateId, String orderNo,
                                      String departureCity, String budgetLevel, String theme) {
        return new FlashSaleMessage(userId, templateId, orderNo, LocalDateTime.now(),
                departureCity, budgetLevel, theme);
    }

    /** 兼容旧调用（无偏好）。 */
    public static FlashSaleMessage of(Long userId, Long templateId, String orderNo) {
        return of(userId, templateId, orderNo, null, null, null);
    }
}
