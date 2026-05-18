package com.ctrip.content.converter;

import com.ctrip.content.dto.request.CreateCommentRequest;
import com.ctrip.content.dto.response.CommentTreeResponse;
import com.ctrip.content.entity.Comment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论 Entity ↔ DTO 转换器。
 */
public class CommentConverter {

    private CommentConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将创建请求转换为 Entity。
     *
     * @param guideId 攻略 ID
     * @param userId  评论者用户 ID
     * @param request 创建请求
     */
    public static Comment toEntity(Long guideId, Long userId, CreateCommentRequest request) {
        return Comment.builder()
                .guideId(guideId)
                .userId(userId)
                .parentId(request.parentId())
                .content(request.content())
                .likeCount(0)
                .build();
    }

    /**
     * 将单个 Entity 转换为树形响应 DTO（无子节点）。
     */
    public static CommentTreeResponse toTreeResponse(Comment entity) {
        return new CommentTreeResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getParentId(),
                entity.getContent(),
                entity.getLikeCount(),
                entity.getCreatedAt(),
                List.of()
        );
    }

    /**
     * 将评论列表转换为树形结构。
     *
     * <p>按 parentId 分组：parentId = null 为根评论，非 null 为回复。
     * 将所有回复挂载到对应的根评论 children 下。
     */
    public static List<CommentTreeResponse> toTreeList(List<Comment> comments) {
        // 按 parentId 分组
        Map<Long, List<Comment>> repliesByParent = comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 根评论（parentId = null）
        return comments.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> {
                    List<CommentTreeResponse> children = repliesByParent
                            .getOrDefault(root.getId(), List.of())
                            .stream()
                            .map(CommentConverter::toTreeResponse)
                            .toList();
                    return new CommentTreeResponse(
                            root.getId(),
                            root.getUserId(),
                            root.getParentId(),
                            root.getContent(),
                            root.getLikeCount(),
                            root.getCreatedAt(),
                            children
                    );
                })
                .toList();
    }
}
