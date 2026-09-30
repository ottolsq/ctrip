package com.ctrip.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点实体，映射数据库 attractions 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("attractions")
public class Attraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long destinationId;    // 所属目的地 ID（外键）

    private String name;           // 景点名称
    private String description;    // 景点简介
    private String location;       // 详细地址/坐标

    private BigDecimal ticketPrice; // 门票价格，NULL 表示免费或未设置
    private String coverUrl;       // 封面图 URL
    private String imageUrls;      // 多图 JSON 数组

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
