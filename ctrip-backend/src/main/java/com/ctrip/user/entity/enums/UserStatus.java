package com.ctrip.user.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户账号状态。
 * 数据库存储对应的整型值（TINYINT），MyBatis Plus 通过 @EnumValue 自动完成 int ↔ 枚举转换。
 */
public enum UserStatus {

    UNVERIFIED(0),  // 注册后尚未完成邮箱/手机验证
    ACTIVE(1),      // 正常可用
    SUSPENDED(2),   // 被管理员封禁
    DELETED(3);     // 软删除，数据保留但账号不可用

    // 告知 MyBatis Plus 用此字段的值与数据库列做映射
    @EnumValue
    private final int value;

    UserStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
