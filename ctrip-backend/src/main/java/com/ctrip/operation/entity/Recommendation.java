package com.ctrip.operation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.entity.enums.RecommendationTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 首页推荐位，映射 recommendation 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recommendation")
public class Recommendation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String slot;

    private String title;

    private RecommendationTargetType targetType;

    private Long targetId;

    private Integer sortOrder;

    private DisplayStatus status;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
