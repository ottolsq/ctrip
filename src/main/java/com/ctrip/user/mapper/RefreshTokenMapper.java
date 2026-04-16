package com.ctrip.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.user.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * Refresh Token 数据访问接口。
 *
 * <p>主要使用场景：
 * <ul>
 *   <li>颁发：{@code insert(refreshToken)} — 存储新的 token 哈希
 *   <li>验证：{@code selectOne(wrapper)} — 按 tokenHash 查询，检查是否过期或已吊销
 *   <li>轮换：先 {@code updateById} 将旧行标记 revoked=true，再 {@code insert} 新行
 *   <li>登出：{@code updateById} 将指定行标记 revoked=true
 * </ul>
 *
 * <p>复杂查询（如批量吊销某用户所有 token）可在此接口中新增方法并配合 Wrapper 使用。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
