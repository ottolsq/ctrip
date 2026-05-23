package com.ctrip.user.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户角色。
 * 对应数据库 users.role TINYINT 列，默认值 0（USER）。
 */
public enum UserRole {

    USER(0, "普通用户"),
    ADMIN(1, "管理员"),
    CONTENT_OPERATOR(2, "内容运维");

    @EnumValue
    private final int code;
    private final String label;

    UserRole(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
