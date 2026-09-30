package com.ctrip.itinerary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏表 — 通用收藏（行程、攻略、目的地）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection")
public class Collection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private CollectionTargetType targetType;

    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
