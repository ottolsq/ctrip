package com.ctrip.operation.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 运营内容展示状态（轮播图/推荐位共用）。
 */
public enum DisplayStatus {
    ENABLED(0),
    DISABLED(1);

    @EnumValue
    private final int value;

    DisplayStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
