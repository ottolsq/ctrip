package com.ctrip.content.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.dto.response.AdminCommentResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.impl.CommentServiceImpl;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理端评论服务单元测试")
class CommentAdminServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CacheService cacheService;

    private CommentServiceImpl commentService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Comment.class);
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(commentMapper, guideMapper, userMapper, cacheService);
    }

    private Comment comment(Long id, Long guideId, Long userId, Long parentId, String content) {
        return Comment.builder()
                .id(id).guideId(guideId).userId(userId).parentId(parentId)
                .content(content).likeCount(0)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("评论列表：分页并装配攻略标题与用户信息")
    void listAdminComments_shouldFilterAndEnrichGuideAndUser() {
        Comment comment = comment(1L, 10L, 2L, null, "违规评论");
        when(commentMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setRecords(List.of(comment));
            page.setTotal(1L);
            return page;
        });
        when(guideMapper.selectBatchIds(any())).thenReturn(List.of(
                Guide.builder().id(10L).title("杭州攻略").build()));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().id(2L).username("张三").avatarUrl("avatar").build()));

        Page<AdminCommentResponse> result =
                commentService.listAdminComments(1, 10, 10L, null, "违规", null, null);

        assertThat(result.getRecords()).hasSize(1);
        AdminCommentResponse item = result.getRecords().get(0);
        assertThat(item.guideTitle()).isEqualTo("杭州攻略");
        assertThat(item.userName()).isEqualTo("张三");
        assertThat(item.content()).isEqualTo("违规评论");
    }

    @Test
    @DisplayName("删除评论：级联子回复并清理攻略缓存")
    void adminDeleteComment_shouldCascadeDescendantsAndEvictGuideCache() {
        Comment parent = comment(1L, 10L, 2L, null, "父评论");
        Comment child = comment(2L, 10L, 3L, 1L, "子评论");
        when(commentMapper.selectById(1L)).thenReturn(parent);
        when(commentMapper.selectList(any())).thenReturn(List.of(child), List.of());

        commentService.adminDeleteComment(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Comment>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentMapper).delete(captor.capture());
        // 触发 SQL 片段生成后，MyBatis Plus 才会填充参数表
        captor.getValue().getSqlSegment();
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(1L, 2L);
        verify(cacheService).evict("guide:10");
    }

    @Test
    @DisplayName("删除评论：三级回复也能级联")
    void adminDeleteComment_depth3_shouldCascadeAllDescendants() {
        Comment parent = comment(1L, 10L, 2L, null, "根评论");
        Comment child = comment(2L, 10L, 3L, 1L, "二级回复");
        Comment grandchild = comment(3L, 10L, 4L, 2L, "三级回复");
        when(commentMapper.selectById(1L)).thenReturn(parent);
        when(commentMapper.selectList(any()))
                .thenReturn(List.of(child), List.of(grandchild), List.of());

        commentService.adminDeleteComment(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Comment>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentMapper).delete(captor.capture());
        captor.getValue().getSqlSegment();
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(1L, 2L, 3L);
    }

    @Test
    @DisplayName("删除不存在的评论：404")
    void adminDeleteComment_missing_shouldThrow() {
        when(commentMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.adminDeleteComment(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
