package com.ctrip.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.content.entity.enums.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 目的地实体，映射数据库 destinations 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("destinations")
public class Destination {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;           // 目的地名称，如 "东京"、"京都"
    private String country;        // 所属国家
    private String province;       // 所属省份/州（国内目的地用）

    private String description;    // 目的地简介

    // MyBatis Plus 通过 @EnumValue 自动将枚举与数据库 TINYINT 互转
    private Season bestSeason;     // 最佳旅游季节

    private String coverUrl;       // 封面图 URL
    private String imageUrls;      // 多图 JSON 数组，如 ["url1","url2"]

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
