package com.ctrip.blindbox.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 盲盒订单状态枚举。
 *
 * <h3>订单生命周期（第3轮异步开盒改造后）</h3>
 * <pre>
 * PENDING ──┬──→ PAID ──→ PROCESSING ──→ OPENED
 *    ↓           ↓            ↓(AI失败重试)
 * CANCELLED   REFUNDED      PAID（回退重试）
 * </pre>
 *
 * <p>PROCESSING(5) 为第3轮异步开盒新增状态：表示已支付但 AI 方案生成中，
 * 前端看到此状态应轮询等待直至 OPENED。
 */
public enum OrderStatus {
    PENDING(0),
    PAID(1),
    OPENED(2),
    REFUNDED(3),
    CANCELLED(4),
    /** 已支付，AI 方案生成中（异步开盒中间状态） */
    PROCESSING(5);

    @EnumValue
    private final int value;

    OrderStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
