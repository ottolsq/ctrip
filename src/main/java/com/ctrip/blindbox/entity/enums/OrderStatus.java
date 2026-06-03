package com.ctrip.blindbox.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 盲盒订单状态枚举。
 *
 * <p>订单生命周期状态流转：
 * <pre>
 * PENDING → PAID → OPENED
 *   ↓
 * CANCELLED（超时或用户主动取消）
 *   ↓
 * REFUNDED（退款）
 * </pre>
 */
public enum OrderStatus {
    PENDING(0),
    PAID(1),
    OPENED(2),
    REFUNDED(3),
    CANCELLED(4);

    @EnumValue
    private final int value;

    OrderStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
