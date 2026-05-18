package com.ctrip.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.content.entity.Attraction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点数据访问接口。
 */
@Mapper
public interface AttractionMapper extends BaseMapper<Attraction> {
}
