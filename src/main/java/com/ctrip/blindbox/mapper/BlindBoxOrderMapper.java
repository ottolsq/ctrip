package com.ctrip.blindbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盲盒订单 Mapper。
 */
@Mapper
public interface BlindBoxOrderMapper extends BaseMapper<BlindBoxOrder> {
}
