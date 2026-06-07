package com.ctrip.content.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.CommentTreeResponse;
import com.ctrip.content.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器。
 *
 * <p>公开接口：评论列表（树形）
 * 需 JWT 认证：发表评论、删除评论
 */
@RestController
@RequestMapping("/api/v1/guides/{guideId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentTreeResponse>>> list(@PathVariable Long guideId) {
        List<CommentTreeResponse> result = commentService.listComments(guideId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentTreeResponse>> create(
            @PathVariable Long guideId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentTreeResponse result = commentService.createComment(guideId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
