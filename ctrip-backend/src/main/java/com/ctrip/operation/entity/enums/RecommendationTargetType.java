package com.ctrip.operation.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 推荐位目标类型。
 */
public enum RecommendationTargetType {
    GUIDE(0),
    DESTINATION(1),
    BLIND_BOX_TEMPLATE(2);

    @EnumValue
    private final int value;

    RecommendationTargetType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
