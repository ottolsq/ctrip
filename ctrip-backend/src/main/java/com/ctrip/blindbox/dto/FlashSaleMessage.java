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
        LocalDateTime timestamp
) {
    /**
     * 工厂方法：创建秒杀消息，时间戳自动设为当前时间。
     *
     * @param userId     用户 ID
     * @param templateId 盲盒模板 ID
     * @param orderNo    预生成的订单号
     * @return 秒杀消息实例
     */
    public static FlashSaleMessage of(Long userId, Long templateId, String orderNo) {
        return new FlashSaleMessage(userId, templateId, orderNo, LocalDateTime.now());
    }
}
