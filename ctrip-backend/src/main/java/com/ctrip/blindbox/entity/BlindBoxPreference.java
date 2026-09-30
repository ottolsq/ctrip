package com.ctrip.blindbox.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.blindbox.entity.enums.BudgetLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 盲盒预选参数实体。
 *
 * <p>用户在创建订单时提交的偏好信息，用于盲盒方案生成：
 * 出发城市、预算等级、旅行主题等。
 * 与订单一对一关联，在同一事务中保存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blind_box_preference")
public class BlindBoxPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的订单ID */
    private Long orderId;

    /** 出发城市 */
    private String departureCity;

    /** 预算等级：ECONOMY / STANDARD / LUXURY */
    private BudgetLevel budgetLevel;

    /** 旅行主题（可选） */
    private String theme;

    /** AI 分析图片特征标签（暂不使用） */
    private String imageTags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
