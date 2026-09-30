package com.ctrip.itinerary.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 行程状态枚举。
 */
public enum ItineraryStatus {
    DRAFT(0),
    PUBLISHED(1),
    ARCHIVED(2);

    @EnumValue
    private final int value;

    ItineraryStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
