package com.ctrip.blindbox.prize;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 盲盒奖品档位。
 */
public enum PrizeTier {
    COMMON(0),
    RARE(1),
    LEGENDARY(2);

    @EnumValue
    private final int value;

    PrizeTier(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean atLeastRare() {
        return this == RARE || this == LEGENDARY;
    }
}
