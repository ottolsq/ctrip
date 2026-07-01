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
import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserRole;
import com.ctrip.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类。
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final GuideMapper guideMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(CommentMapper commentMapper, GuideMapper guideMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.guideMapper = guideMapper;
        this.userMapper = userMapper;
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

        // 批量查询用户信息
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String[]> userInfoMap = batchQueryUserInfo(userIds);

        return CommentConverter.toTreeList(comments, userInfoMap);
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

        // 查询用户信息
        User user = userMapper.selectById(userId);
        String userName = user != null ? user.getUsername() : "匿名";
        String userAvatar = user != null ? user.getAvatarUrl() : null;

        return CommentConverter.toTreeResponse(requireComment(comment.getId()), userName, userAvatar);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = requireComment(commentId);
        checkCommentOwner(comment, userId);

        // 级联删除：收集该评论及其所有子孙评论 ID，一次性删除
        List<Long> idsToDelete = new java.util.ArrayList<>(collectDescendants(commentId));
        idsToDelete.add(commentId);

        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .in(Comment::getId, idsToDelete));
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
     * 校验操作权限：评论作者或 ADMIN 可操作。
     */
    private void checkCommentOwner(Comment comment, Long userId) {
        if (comment.getUserId().equals(userId)) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AuthenticationException("无权操作他人评论");
        }
    }

    /**
     * 递归收集指定评论的所有子孙评论 ID。
     */
    private List<Long> collectDescendants(Long parentId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, parentId)
                .select(Comment::getId);
        List<Comment> children = commentMapper.selectList(wrapper);
        if (children.isEmpty()) {
            return List.of();
        }
        List<Long> ids = children.stream().map(Comment::getId).collect(java.util.stream.Collectors.toList());
        for (Long childId : ids) {
            ids.addAll(collectDescendants(childId));
        }
        return ids;
    }

    /**
     * 批量查询用户信息，返回 Map：userId -> [userName, userAvatar]
     */
    private Map<Long, String[]> batchQueryUserInfo(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> new String[]{u.getUsername(), u.getAvatarUrl()},
                        (a, b) -> a
                ));
    }
}
