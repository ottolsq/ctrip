package com.ctrip.blindbox.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 盲盒结果实体。
 *
 * <p>开盒后生成的旅行方案，包含目的地、主题、完整行程 JSON、分享码等。
 * resultText 为完整的旅行方案 JSON，openedAt 记录开盒时间。
 * shareCode 为 6 位 Base62 短码，用于公开分享链接。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blind_box_result")
public class BlindBoxResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的订单ID */
    private Long orderId;

    /** 目的地名称 */
    private String destination;

    /** 目的地ID（FK → destination） */
    private Long destinationId;

    /** 旅行主题（美食/海滨/古镇/滑雪/亲子等） */
    private String theme;

    /** 完整旅行方案 JSON（目的地/行程/酒店/交通/预算） */
    private String resultText;

    /** AI 生成结果图 URL（暂不使用） */
    private String resultImageUrl;

    /** 分享链接短码（6位Base62） */
    private String shareCode;

    /** 分享链接过期时间 */
    private LocalDateTime shareExpiresAt;

    /** 开盒时间 */
    private LocalDateTime openedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
