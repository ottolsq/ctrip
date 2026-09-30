package com.ctrip.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体，映射数据库 refresh_tokens 表。
 *
 * <p>安全设计：
 * <ul>
 *   <li>数据库只存储原始 token 的 SHA-256 哈希值（tokenHash），不存原始值。
 *   <li>原始 token 仅在颁发时通过响应体返回给客户端，之后服务端无法还原。
 *   <li>刷新流程：客户端发送原始值 → 服务端计算哈希 → 按哈希查库 → 校验通过后执行
 *       token 轮换（旧行标记 revoked=true，插入新行）。
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("refresh_tokens")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的用户 ID，对应外键 fk_rt_user_id（ON DELETE CASCADE）
    private Long userId;

    // 原始 token 的 SHA-256 哈希（hex 编码，64字符），不存原始值
    private String tokenHash;

    // 客户端设备信息，可选，用于多设备管理时辅助展示
    private String deviceInfo;

    // token 颁发时间，INSERT 时由 MetaObjectHandler 自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime issuedAt;

    // token 过期时间，过期后即使未被吊销也不可使用
    private LocalDateTime expiresAt;

    // 是否已被吊销（主动登出或 token 轮换时置为 true）
    private Boolean revoked;

    // 吊销时间，revoked=false 时此字段为 null
    private LocalDateTime revokedAt;
}
