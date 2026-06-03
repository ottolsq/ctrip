package com.ctrip.blindbox.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盲盒订单实体。
 *
 * <p>记录用户购买盲盒的订单信息，包含订单编号、状态、支付信息等。
 * 订单号格式：BByyyyMMddXXXX（如 BB202606020001）。
 * 创建后 15 分钟未支付自动超时取消。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blind_box_order")
public class BlindBoxOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID */
    private Long userId;

    /** 关联的盲盒模板ID */
    private Long templateId;

    /** 订单编号（业务主键，格式：BB + 日期 + 序号） */
    private String orderNo;

    /** 订单状态 */
    private OrderStatus status;

    /** 实际支付金额 */
    private BigDecimal payAmount;

    /** 支付方式：ALIPAY / WECHAT_PAY */
    private String payMethod;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付超时时间（创建后15分钟） */
    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
