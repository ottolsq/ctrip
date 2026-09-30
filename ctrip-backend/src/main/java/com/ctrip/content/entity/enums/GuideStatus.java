package com.ctrip.content.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 攻略发布状态。
 * 对应数据库 guides.status TINYINT 列。
 */
public enum GuideStatus {

    DRAFT(0),     // 草稿，仅作者可见
    PUBLISHED(1), // 已发布，公开可见
    REJECTED(2);  // 审核驳回

    @EnumValue
    private final int value;

    GuideStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
