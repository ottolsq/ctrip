package com.ctrip.itinerary.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 行程项类型枚举。
 */
public enum ItemType {
    HOTEL(0),
    ATTRACTION(1),
    TRANSPORT(2),
    FOOD(3),
    ACTIVITY(4);

    @EnumValue
    private final int value;

    ItemType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
