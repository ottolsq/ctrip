package com.ctrip.blindbox.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盲盒模板实体。
 *
 * <p>定义盲盒的名称、类型、价格、库存、状态等基础信息。
 * 日常盲盒 stock=-1 不限量，限定盲盒 stock>0 限量抢购。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blind_box_template")
public class BlindBoxTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盲盒名称 */
    private String name;

    /** 盲盒类型：DAILY 日常 / LIMITED 限定 */
    private BlindBoxType type;

    /** 价格（单位：元） */
    private BigDecimal price;

    /** 库存：-1=无限，>=0=限量 */
    private Integer stock;

    /** 规则配置 JSON（活动起止时间、折扣、描述等） */
    private String ruleConfig;

    /** 模板状态：ACTIVE 上架 / INACTIVE 下架 */
    private TemplateStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
