package com.ctrip.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.user.entity.enums.Gender;
import com.ctrip.user.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体，映射数据库 users 表。
 *
 * <p>设计约定：
 * <ul>
 *   <li>此类仅在持久层（Mapper）和服务层内部流转，不直接暴露给 Controller。
 *   <li>Controller 层需通过 UserConverter 将其转换为 DTO 后再返回。
 *   <li>密码以 BCrypt 哈希形式存储于 passwordHash，原始密码绝不落库。
 * </ul>
 */
@Data               // Lombok：生成 getter/setter/equals/hashCode/toString
@Builder            // Lombok：提供链式构建器，便于测试和对象创建
@NoArgsConstructor  // MyBatis Plus 反射实例化时需要无参构造器
@AllArgsConstructor // 配合 @Builder 使用
@TableName("users") // 指定映射的数据库表名
public class User {

    // IdType.AUTO 对应数据库自增主键（AUTO_INCREMENT）
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    // 仅存储 BCrypt 哈希值（强度 12），原始密码不落库
    private String passwordHash;

    private String avatarUrl;

    // MyBatis Plus 通过 @EnumValue 自动将枚举与数据库 TINYINT 互转
    private Gender gender;

    private LocalDate birthday;

    private String realName;  // 实名认证姓名

    private UserStatus status;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private LocalDateTime lastLoginAt;

    // FieldFill.INSERT：仅在 INSERT 时由 MetaObjectHandler 自动填充，无需手动赋值
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // FieldFill.INSERT_UPDATE：INSERT 和 UPDATE 时均自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
