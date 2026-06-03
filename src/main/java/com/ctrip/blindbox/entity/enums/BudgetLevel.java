package com.ctrip.blindbox.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 预算等级枚举。
 *
 * <p>用于盲盒预选参数，决定生成方案的价格区间：
 * <ul>
 *   <li>ECONOMY — 经济型（低价位酒店/交通）</li>
 *   <li>STANDARD — 标准型（中等价位）</li>
 *   <li>LUXURY — 豪华型（高端酒店/交通）</li>
 * </ul>
 */
public enum BudgetLevel {
    ECONOMY(0),
    STANDARD(1),
    LUXURY(2);

    @EnumValue
    private final int value;

    BudgetLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
