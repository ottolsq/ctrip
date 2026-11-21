package com.ctrip.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.operation.entity.Recommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐位 Mapper。
 */
@Mapper
public interface RecommendationMapper extends BaseMapper<Recommendation> {
}
