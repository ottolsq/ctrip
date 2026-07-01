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
     *
     * @param entity    评论实体
     * @param userName  用户名字
     * @param userAvatar 用户头像
     */
    public static CommentTreeResponse toTreeResponse(Comment entity, String userName, String userAvatar) {
        return new CommentTreeResponse(
                entity.getId(),
                entity.getUserId(),
                userName,
                userAvatar,
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
     *
     * @param comments      评论列表
     * @param userInfoMap   用户信息Map（userId -> [userName, userAvatar]）
     */
    public static List<CommentTreeResponse> toTreeList(List<Comment> comments, 
                                                        Map<Long, String[]> userInfoMap) {
        // 按 parentId 分组
        Map<Long, List<Comment>> repliesByParent = comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 根评论（parentId = null）
        return comments.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> {
                    String[] rootUserInfo = userInfoMap.getOrDefault(root.getUserId(), new String[]{"匿名", null});
                    List<CommentTreeResponse> children = repliesByParent
                            .getOrDefault(root.getId(), List.of())
                            .stream()
                            .map(child -> {
                                String[] childUserInfo = userInfoMap.getOrDefault(child.getUserId(), new String[]{"匿名", null});
                                return toTreeResponse(child, childUserInfo[0], childUserInfo[1]);
                            })
                            .toList();
                    return new CommentTreeResponse(
                            root.getId(),
                            root.getUserId(),
                            rootUserInfo[0],
                            rootUserInfo[1],
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
