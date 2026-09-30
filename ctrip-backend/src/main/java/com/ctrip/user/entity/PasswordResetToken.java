package com.ctrip.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 密码重置令牌实体，映射数据库 password_reset_tokens 表。
 *
 * <p>工作流程：
 * <ol>
 *   <li>用户发起找回密码请求，系统生成 6 位 OTP（一次性密码）。
 *   <li>将 OTP 的 SHA-256 哈希存入此表，原始 OTP 通过短信或邮件发送给用户。
 *   <li>用户提交 OTP + 新密码时，服务端对 OTP 计算哈希后查此表校验。
 *   <li>校验通过后将 used 置为 true，防止重放攻击。
 * </ol>
 *
 * <p>注意：此表没有 created_at 列，不需要 @TableField(fill = ...) 自动填充。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("password_reset_tokens")
public class PasswordResetToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的用户 ID，对应外键 fk_prt_user_id（ON DELETE CASCADE）
    private Long userId;

    // OTP 原始值的 SHA-256 哈希（hex 编码，64字符），不存原始 OTP
    private String tokenHash;

    // 发送渠道，取值为 "EMAIL" 或 "SMS"
    private String channel;

    // 令牌过期时间，过期后即使未使用也不可用（建议有效期 10 分钟）
    private LocalDateTime expiresAt;

    // 是否已使用，校验成功后立即置 true，防止重放攻击
    private Boolean used;
}
