package com.ctrip.content.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.config.CacheConfig;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.dto.response.AdminGuideListItemResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.impl.AdminGuideServiceImpl;
import com.ctrip.messaging.publisher.EventPublisher;
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
import org.redisson.api.RBloomFilter;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理端攻略审核服务单元测试")
class AdminGuideServiceImplTest {

    @Mock
    private GuideMapper guideMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DestinationMapper destinationMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CacheService cacheService;
    @Mock
    private RBloomFilter<String> guideBloomFilter;
    @Mock
    private EventPublisher eventPublisher;

    private AdminGuideServiceImpl adminGuideService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Destination.class);
        TableInfoHelper.initTableInfo(assistant, Comment.class);
    }

    @BeforeEach
    void setUp() {
        adminGuideService = new AdminGuideServiceImpl(
                guideMapper, userMapper, destinationMapper, commentMapper,
                cacheService, guideBloomFilter, eventPublisher);
        lenient().when(userMapper.selectById(10L))
                .thenReturn(User.builder().id(10L).username("author").build());
        lenient().when(destinationMapper.selectById(3L))
                .thenReturn(Destination.builder().id(3L).name("杭州").build());
        lenient().when(commentMapper.selectCount(any())).thenReturn(0L);
    }

    private Guide guide(GuideStatus status) {
        return Guide.builder()
                .id(1L)
                .authorId(10L)
                .title("待审攻略")
                .content("<p>正文</p>")
                .destinationId(3L)
                .status(status)
                .rejectReason("旧理由")
                .viewCount(5)
                .likeCount(1)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("审核列表：映射作者与驳回理由")
    void listGuides_pendingFilter_shouldMapAuthorAndReason() {
        when(guideMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Guide> page = invocation.getArgument(0);
            page.setRecords(List.of(guide(GuideStatus.PENDING)));
            page.setTotal(1L);
            return page;
        });
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().id(10L).username("author").build()));

        Page<AdminGuideListItemResponse> result =
                adminGuideService.listGuides(1, 10, GuideStatus.PENDING, "待审", null);

        assertThat(result.getRecords()).hasSize(1);
        AdminGuideListItemResponse item = result.getRecords().get(0);
        assertThat(item.authorName()).isEqualTo("author");
        assertThat(item.status()).isEqualTo("PENDING");
        assertThat(item.rejectReason()).isEqualTo("旧理由");
    }

    @Test
    @DisplayName("通过审核：置 PUBLISHED、加布隆、清缓存、发事件")
    void approve_pending_shouldPublishAddBloomEvictAndPublishEvent() {
        when(guideMapper.selectById(1L)).thenReturn(guide(GuideStatus.PENDING));

        adminGuideService.approve(1L, 99L);

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.PUBLISHED);
        assertThat(captor.getValue().getRejectReason()).isNull();
        assertThat(captor.getValue().getReviewedBy()).isEqualTo(99L);
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
        verify(guideBloomFilter).add(CacheConfig.blogKey(1L));
        verify(cacheService).evict("guide:1");
        verify(cacheService).evictByPattern("guide:list:*");
        verify(eventPublisher).publishContentAuditEvent(
                eq("GUIDE_APPROVED"), eq(1L), eq(10L), eq(99L), isNull());
    }

    @Test
    @DisplayName("非 PENDING 不可通过")
    void approve_nonPending_shouldThrow() {
        when(guideMapper.selectById(1L)).thenReturn(guide(GuideStatus.PUBLISHED));

        assertThatThrownBy(() -> adminGuideService.approve(1L, 99L))
                .isInstanceOf(BusinessException.class);
        verify(guideMapper, never()).updateById(any(Guide.class));
        verifyNoInteractions(guideBloomFilter);
    }

    @Test
    @DisplayName("驳回：写入理由与审核人并发送事件")
    void reject_pending_shouldSetReasonAndPublishEvent() {
        when(guideMapper.selectById(1L)).thenReturn(guide(GuideStatus.PENDING));

        adminGuideService.reject(1L, 99L, "内容不符合规范");

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.REJECTED);
        assertThat(captor.getValue().getRejectReason()).isEqualTo("内容不符合规范");
        assertThat(captor.getValue().getReviewedBy()).isEqualTo(99L);
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
        verify(cacheService).evict("guide:1");
        verify(eventPublisher).publishContentAuditEvent(
                eq("GUIDE_REJECTED"), eq(1L), eq(10L), eq(99L), eq("内容不符合规范"));
    }

    @Test
    @DisplayName("驳回理由为空：拒绝")
    void reject_blankReason_shouldThrow() {
        assertThatThrownBy(() -> adminGuideService.reject(1L, 99L, "  "))
                .isInstanceOf(BusinessException.class);
        verify(guideMapper, never()).updateById(any(Guide.class));
    }

    @Test
    @DisplayName("审核详情：PENDING 可直接读取正文，不走缓存")
    void getDetail_pending_shouldReturnContentWithoutCache() {
        when(guideMapper.selectById(1L)).thenReturn(guide(GuideStatus.PENDING));

        GuideResponse response = adminGuideService.getDetail(1L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.content()).isEqualTo("<p>正文</p>");
        assertThat(response.rejectReason()).isEqualTo("旧理由");
        verifyNoInteractions(cacheService);
        verifyNoInteractions(guideBloomFilter);
    }
}
