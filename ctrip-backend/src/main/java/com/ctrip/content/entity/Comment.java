package com.ctrip.content.entity;

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
 * 评论实体，映射数据库 comments 表。
 * 支持嵌套评论：parentId 指向父评论 ID，NULL 表示顶级评论。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long guideId;          // 所属攻略 ID（外键）
    private Long userId;           // 评论者用户 ID

    // 自关联：NULL 表示顶级评论，非 NULL 表示回复某条评论
    private Long parentId;

    private String content;        // 评论内容
    private Integer likeCount;     // 点赞次数

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
