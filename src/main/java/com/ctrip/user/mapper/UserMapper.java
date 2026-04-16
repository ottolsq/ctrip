package com.ctrip.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口。
 *
 * <p>继承 MyBatis Plus 的 {@link BaseMapper}，自动获得以下常用方法，无需手写 SQL：
 * <ul>
 *   <li>{@code selectById(id)} — 按主键查询
 *   <li>{@code selectOne(wrapper)} — 按条件查询单条（如按 email/phone 查询）
 *   <li>{@code insert(user)} — 插入
 *   <li>{@code updateById(user)} — 按主键更新（仅更新非 null 字段）
 *   <li>{@code deleteById(id)} — 按主键删除
 * </ul>
 *
 * <p>复杂查询（多表关联等）可在此接口中新增方法，并在
 * {@code resources/mapper/UserMapper.xml} 中编写对应 SQL。
 */
@Mapper // 告知 Spring 将此接口注册为 MyBatis Mapper Bean
public interface UserMapper extends BaseMapper<User> {
}
