package com.ctrip.itinerary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.itinerary.entity.ItineraryItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItineraryItemMapper extends BaseMapper<ItineraryItem> {
}
