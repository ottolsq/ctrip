package com.ctrip.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.content.entity.Guide;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略数据访问接口。
 */
@Mapper
public interface GuideMapper extends BaseMapper<Guide> {
}
