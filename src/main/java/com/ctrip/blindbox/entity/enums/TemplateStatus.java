package com.ctrip.blindbox.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 盲盒模板状态枚举。
 *
 * <p>控制模板是否对用户可见和可购买：
 * <ul>
 *   <li>ACTIVE — 上架中，用户可购买</li>
 *   <li>INACTIVE — 已下架</li>
 * </ul>
 */
public enum TemplateStatus {
    ACTIVE(0),
    INACTIVE(1);

    @EnumValue
    private final int value;

    TemplateStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
