package com.ctrip.itinerary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.itinerary.entity.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行程明细表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("itinerary_item")
public class ItineraryItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itineraryDayId;

    private ItemType type;

    private String name;

    private String location;

    private String timeSlot;

    private String description;

    private Integer sortOrder;
}
