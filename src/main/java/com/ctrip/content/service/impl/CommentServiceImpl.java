package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.converter.CommentConverter;
import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.CommentTreeResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务实现类。
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final GuideMapper guideMapper;

    public CommentServiceImpl(CommentMapper commentMapper, GuideMapper guideMapper) {
        this.commentMapper = commentMapper;
        this.guideMapper = guideMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentTreeResponse> listComments(Long guideId) {
        requirePublishedGuide(guideId);

        // 查询该攻略的所有评论，按创建时间排序
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getGuideId, guideId)
                .orderByAsc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(wrapper);

        return CommentConverter.toTreeList(comments);
    }

    @Override
    @Transactional
    public CommentTreeResponse createComment(Long guideId, Long userId, CreateCommentRequest request) {
        requirePublishedGuide(guideId);

        // 如果是回复，校验父评论存在且属于该攻略
        if (request.parentId() != null) {
            Comment parent = commentMapper.selectById(request.parentId());
            if (parent == null) {
                throw new ResourceNotFoundException("父评论不存在");
            }
            if (!parent.getGuideId().equals(guideId)) {
                throw new ResourceNotFoundException("父评论不属于该攻略");
            }
        }

        Comment comment = CommentConverter.toEntity(guideId, userId, request);
        commentMapper.insert(comment);

        return CommentConverter.toTreeResponse(requireComment(comment.getId()));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = requireComment(commentId);
        checkCommentOwner(comment, userId);

        // 级联删除：删除该评论及其所有子评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .or()
                .eq(Comment::getParentId, commentId));
    }

    // --- 私有辅助 ---

    private Comment requireComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new ResourceNotFoundException("评论不存在：id=" + id);
        }
        return comment;
    }

    private void requirePublishedGuide(Long guideId) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getStatus() != GuideStatus.PUBLISHED) {
            throw new ResourceNotFoundException("攻略不存在：id=" + guideId);
        }
    }

    /**
     * 校验操作权限：仅评论作者本人可操作。
     */
    private void checkCommentOwner(Comment comment, Long userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new AuthenticationException("无权操作他人评论");
        }
    }
}
