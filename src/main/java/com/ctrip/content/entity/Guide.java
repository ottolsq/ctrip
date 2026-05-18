package com.ctrip.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.content.entity.enums.GuideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 攻略实体，映射数据库 guides 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("guides")
public class Guide {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;         // 作者用户 ID（外键关联 users.id）

    private String title;          // 攻略标题
    private String content;        // 攻略正文（Markdown 格式）

    private Long destinationId;    // 关联目的地 ID（可为 NULL，纯经验贴）

    private String coverUrl;       // 封面图 URL
    private String imageUrls;      // 多图 JSON 数组

    // MyBatis Plus 通过 @EnumValue 自动将枚举与数据库 TINYINT 互转
    private GuideStatus status;    // 发布状态

    private Integer viewCount;     // 浏览次数
    private Integer likeCount;     // 点赞次数

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
