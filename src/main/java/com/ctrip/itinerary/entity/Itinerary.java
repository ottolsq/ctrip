package com.ctrip.itinerary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 行程主表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("itinerary")
public class Itinerary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private Long destinationId;

    private LocalDate startDate;

    private LocalDate endDate;

    private ItineraryStatus status;

    private String shareCode;

    private LocalDateTime shareExpiresAt;

    private Integer viewCount;

    private Integer likeCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
