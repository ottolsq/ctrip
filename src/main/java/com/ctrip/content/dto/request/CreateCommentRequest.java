package com.ctrip.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论请求 DTO。
 */
public record CreateCommentRequest(

        /** 评论内容 */
        @NotBlank(message = "评论内容不能为空")
        String content,

        /** 父评论 ID，NULL 表示顶级评论 */
        Long parentId

) {}
