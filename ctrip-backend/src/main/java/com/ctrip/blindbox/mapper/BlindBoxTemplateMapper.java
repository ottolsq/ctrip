package com.ctrip.blindbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 盲盒模板 Mapper。
 */
@Mapper
public interface BlindBoxTemplateMapper extends BaseMapper<BlindBoxTemplate> {

    /**
     * 乐观锁扣减库存（防止超卖）。
     *
     * <p>SQL: UPDATE blind_box_template SET stock = stock - 1 WHERE id = ? AND stock >= 1
     * stock >= 1 条件天然防止超卖，即使并发也只有一行更新成功。
     *
     * @param templateId 模板ID
     * @return 影响行数，=1 表示扣减成功，=0 表示库存不足
     */
    @Update("UPDATE blind_box_template SET stock = stock - 1 WHERE id = #{templateId} AND stock >= 1")
    int decrementStock(@Param("templateId") Long templateId);

    /**
     * 恢复库存（订单取消/退款时调用）。
     *
     * @param templateId 模板ID
     */
    @Update("UPDATE blind_box_template SET stock = stock + 1 WHERE id = #{templateId} AND stock >= 0")
    void incrementStock(@Param("templateId") Long templateId);
}
