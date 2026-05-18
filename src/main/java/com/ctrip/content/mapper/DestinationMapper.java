package com.ctrip.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.content.entity.Destination;
import org.apache.ibatis.annotations.Mapper;

/**
 * 目的地数据访问接口。
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}，复杂查询（如按国家 + 分页）
 * 通过 MyBatis Plus 的 LambdaQueryWrapper 在 Service 层构建。
 */
@Mapper
public interface DestinationMapper extends BaseMapper<Destination> {
}
