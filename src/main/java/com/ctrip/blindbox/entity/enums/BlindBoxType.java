package com.ctrip.blindbox.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 盲盒类型枚举。
 *
 * <p>用于区分盲盒模板的库存策略和购买规则：
 * <ul>
 *   <li>DAILY — 日常盲盒，不限量，随时购买</li>
 *   <li>LIMITED — 限定盲盒，限时限量抢购</li>
 * </ul>
 */
public enum BlindBoxType {
    DAILY(0),
    LIMITED(1);

    @EnumValue
    private final int value;

    BlindBoxType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
