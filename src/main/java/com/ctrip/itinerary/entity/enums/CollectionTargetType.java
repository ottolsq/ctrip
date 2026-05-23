package com.ctrip.itinerary.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 收藏目标类型枚举。
 */
public enum CollectionTargetType {
    ITINERARY(0),
    GUIDE(1),
    DESTINATION(2);

    @EnumValue
    private final int value;

    CollectionTargetType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
