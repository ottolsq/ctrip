package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.AdminCommentResponse;
import com.ctrip.content.dto.response.CommentTreeResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 评论服务接口，负责攻略评论的发表、查询和删除。
 */
public interface CommentService {

    List<CommentTreeResponse> listComments(Long guideId);

    CommentTreeResponse createComment(Long guideId, Long userId, CreateCommentRequest request);

    void deleteComment(Long commentId, Long userId);

    Page<AdminCommentResponse> listAdminComments(int page, int size, Long guideId, Long userId,
                                                 String keyword, LocalDate startDate,
                                                 LocalDate endDate);

    void adminDeleteComment(Long commentId);
}
