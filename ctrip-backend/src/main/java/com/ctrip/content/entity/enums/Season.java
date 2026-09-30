package com.ctrip.content.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 目的地最佳季节。
 * 对应数据库 TINYINT 列，MyBatis Plus 通过 @EnumValue 自动完成枚举 ↔ 整数转换。
 */
public enum Season {

    SPRING(1),      // 春季（3-5月）
    SUMMER(2),      // 夏季（6-8月）
    AUTUMN(3),      // 秋季（9-11月）
    WINTER(4),      // 冬季（12-2月）
    YEAR_ROUND(5);  // 全年适宜

    @EnumValue
    private final int value;

    Season(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
