package com.ctrip.itinerary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行程日程表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("itinerary_day")
public class ItineraryDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itineraryId;

    private Integer dayNumber;

    private String title;

    private Integer sortOrder;
}
