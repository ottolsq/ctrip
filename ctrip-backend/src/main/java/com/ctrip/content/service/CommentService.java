package com.ctrip.content.service;

import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.CommentTreeResponse;

import java.util.List;

/**
 * 评论服务接口，负责攻略评论的发表、查询和删除。
 */
public interface CommentService {

    List<CommentTreeResponse> listComments(Long guideId);

    CommentTreeResponse createComment(Long guideId, Long userId, CreateCommentRequest request);

    void deleteComment(Long commentId, Long userId);
}
