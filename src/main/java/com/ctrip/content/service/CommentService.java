package com.ctrip.content.service;

import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.CommentTreeResponse;

import java.util.List;

/**
 * 评论服务接口，负责攻略评论的发表、查询和删除。
 */
public interface CommentService {

    /**
     * 查询某攻略的评论列表（树形结构）。
     *
     * @param guideId 攻略 ID
     * @return 评论树
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略不存在时
     */
    List<CommentTreeResponse> listComments(Long guideId);

    /**
     * 发表评论（需 JWT 认证，支持回复评论）。
     *
     * @param guideId 攻略 ID
     * @param userId  评论者用户 ID
     * @param request 评论请求
     * @return 创建后的评论
     * @throws com.ctrip.common.exception.ResourceNotFoundException 攻略或父评论不存在时
     */
    CommentTreeResponse createComment(Long guideId, Long userId, CreateCommentRequest request);

    /**
     * 删除评论（需 JWT 认证，仅评论作者或 ADMIN，级联删除子评论）。
     *
     * @param commentId 评论 ID
     * @param userId    当前操作用户 ID
     * @throws com.ctrip.common.exception.ResourceNotFoundException 评论不存在时
     * @throws com.ctrip.common.exception.AuthenticationException 无权操作时
     */
    void deleteComment(Long commentId, Long userId);
}
