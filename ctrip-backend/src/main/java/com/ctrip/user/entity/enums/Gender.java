package com.ctrip.user.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户性别。
 * 对应数据库 users.gender TINYINT 列，默认值 0（未填写）。
 */
public enum Gender {

    UNSPECIFIED(0),  // 用户未填写
    MALE(1),
    FEMALE(2);

    // 告知 MyBatis Plus 用此字段的值与数据库列做映射
    @EnumValue
    private final int value;

    Gender(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
