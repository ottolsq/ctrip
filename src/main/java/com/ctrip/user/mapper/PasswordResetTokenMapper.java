package com.ctrip.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.user.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密码重置令牌数据访问接口。
 *
 * <p>主要使用场景：
 * <ul>
 *   <li>创建：{@code insert(token)} — 存储新的 OTP 哈希
 *   <li>验证：{@code selectOne(wrapper)} — 按 tokenHash 查询，检查是否过期或已使用
 *   <li>标记已用：{@code updateById(token)} — 将 used 置为 true
 * </ul>
 */
@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {
}
