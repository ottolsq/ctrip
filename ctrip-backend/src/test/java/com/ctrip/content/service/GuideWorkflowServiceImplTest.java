package com.ctrip.content.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.content.cache.CacheService;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.dto.response.MyGuideListResponse;
import com.ctrip.content.entity.Comment;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.impl.GuideServiceImpl;
import com.ctrip.messaging.publisher.EventPublisher;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
@DisplayName("攻略作者工作流单元测试")
class GuideWorkflowServiceImplTest {

    @Mock
    private GuideMapper guideMapper;
    @Mock
    private DestinationMapper destinationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CacheService cacheService;
    @Mock
    private RBloomFilter<String> guideBloomFilter;
    @Mock
    private EventPublisher eventPublisher;

    private GuideServiceImpl guideService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, Destination.class);
        TableInfoHelper.initTableInfo(assistant, Comment.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        guideService = new GuideServiceImpl(
                guideMapper, destinationMapper, userMapper, commentMapper,
                cacheService, guideBloomFilter, new ObjectMapper(), eventPublisher);
        lenient().when(destinationMapper.selectById(3L))
                .thenReturn(Destination.builder().id(3L).name("杭州").build());
        lenient().when(userMapper.selectById(10L))
                .thenReturn(User.builder().id(10L).username("author").build());
        lenient().when(commentMapper.selectCount(any())).thenReturn(0L);
    }

    private Guide guide(Long id, Long authorId, GuideStatus status) {
        return Guide.builder()
                .id(id)
                .authorId(authorId)
                .title("title" + id)
                .content("<p>content</p>")
                .destinationId(3L)
                .status(status)
                .viewCount(10)
                .likeCount(2)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private void stubUpdateMutatesCurrent(AtomicReference<Guide> current) {
        when(guideMapper.updateById(any(Guide.class))).thenAnswer(invocation -> {
            Guide update = invocation.getArgument(0);
            Guide target = current.get();
            if (update.getStatus() != null) {
                target.setStatus(update.getStatus());
            }
            target.setRejectReason(update.getRejectReason());
            target.setReviewedBy(update.getReviewedBy());
            target.setReviewedAt(update.getReviewedAt());
            if (update.getTitle() != null) {
                target.setTitle(update.getTitle());
            }
            if (update.getContent() != null) {
                target.setContent(update.getContent());
            }
            return 1;
        });
    }

    @Test
    @DisplayName("创建攻略：固定为 DRAFT")
    void createGuide_shouldForceDraft() {
        AtomicReference<Guide> inserted = new AtomicReference<>();
        when(guideMapper.insert(any(Guide.class))).thenAnswer(invocation -> {
            Guide guide = invocation.getArgument(0);
            guide.setId(1L);
            inserted.set(guide);
            return 1;
        });
        when(guideMapper.selectById(1L)).thenAnswer(invocation -> inserted.get());

        guideService.createGuide(10L, new CreateGuideRequest(
                "title", "<p>content</p>", 3L, null, null));

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.DRAFT);
    }

    @Test
    @DisplayName("提交草稿：变为 PENDING，清理审核字段并发送事件")
    void submitGuide_draft_shouldSetPendingAndPublishSubmittedEvent() {
        AtomicReference<Guide> current = new AtomicReference<>(guide(1L, 10L, GuideStatus.DRAFT));
        when(guideMapper.selectById(1L)).thenAnswer(invocation -> current.get());
        stubUpdateMutatesCurrent(current);

        GuideResponse response = guideService.submitGuide(1L, 10L);

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.PENDING);
        assertThat(captor.getValue().getRejectReason()).isNull();
        assertThat(captor.getValue().getReviewedAt()).isNull();
        assertThat(response.status()).isEqualTo("PENDING");
        verify(cacheService).evict("guide:1");
        verify(cacheService).evictByPattern("guide:list:*");
        verify(eventPublisher).publishContentAuditEvent(
                eq("GUIDE_SUBMITTED"), eq(1L), eq(10L), isNull(), isNull());
    }

    @Test
    @DisplayName("已发布攻略不可提交审核")
    void submitGuide_published_shouldThrow() {
        when(guideMapper.selectById(1L))
                .thenReturn(guide(1L, 10L, GuideStatus.PUBLISHED));

        assertThatThrownBy(() -> guideService.submitGuide(1L, 10L))
                .isInstanceOf(BusinessException.class);
        verify(guideMapper, never()).updateById(any(Guide.class));
    }

    @Test
    @DisplayName("编辑草稿：保持 DRAFT 并清理审核字段")
    void updateGuide_draft_shouldStayDraftAndClearReviewFields() {
        Guide draft = guide(1L, 10L, GuideStatus.DRAFT);
        draft.setRejectReason("old reason");
        AtomicReference<Guide> current = new AtomicReference<>(draft);
        when(guideMapper.selectById(1L)).thenAnswer(invocation -> current.get());
        stubUpdateMutatesCurrent(current);

        GuideResponse response = guideService.updateGuide(1L, 10L,
                new UpdateGuideRequest("new title", "<p>new</p>", null, null, null));

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.DRAFT);
        assertThat(captor.getValue().getRejectReason()).isNull();
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("编辑已发布攻略：转为 PENDING 并失效缓存")
    void updateGuide_publishedWithContent_shouldSetPendingAndEvictCaches() {
        AtomicReference<Guide> current =
                new AtomicReference<>(guide(1L, 10L, GuideStatus.PUBLISHED));
        when(guideMapper.selectById(1L)).thenAnswer(invocation -> current.get());
        stubUpdateMutatesCurrent(current);

        GuideResponse response = guideService.updateGuide(1L, 10L,
                new UpdateGuideRequest("new title", null, null, null, null));

        ArgumentCaptor<Guide> captor = ArgumentCaptor.forClass(Guide.class);
        verify(guideMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GuideStatus.PENDING);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(cacheService).evict("guide:1");
        verify(cacheService).evictByPattern("guide:list:*");
        verify(eventPublisher).publishContentAuditEvent(
                eq("GUIDE_SUBMITTED"), eq(1L), eq(10L), isNull(), isNull());
    }

    @Test
    @DisplayName("编辑已发布：缓存失效与事件延迟到事务提交后")
    void updateGuide_published_shouldDeferCacheEvictionUntilAfterCommit() {
        AtomicReference<Guide> current =
                new AtomicReference<>(guide(1L, 10L, GuideStatus.PUBLISHED));
        when(guideMapper.selectById(1L)).thenAnswer(invocation -> current.get());
        stubUpdateMutatesCurrent(current);

        TransactionSynchronizationManager.initSynchronization();
        try {
            guideService.updateGuide(1L, 10L,
                    new UpdateGuideRequest("new title", null, null, null, null));

            verify(cacheService, never()).evict(any());
            verify(eventPublisher, never()).publishContentAuditEvent(
                    any(), any(), any(), any(), any());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(cacheService).evict("guide:1");
            verify(cacheService).evictByPattern("guide:list:*");
            verify(eventPublisher).publishContentAuditEvent(
                    eq("GUIDE_SUBMITTED"), eq(1L), eq(10L), isNull(), isNull());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("审核中的攻略不可编辑")
    void updateGuide_pending_shouldThrow() {
        when(guideMapper.selectById(1L))
                .thenReturn(guide(1L, 10L, GuideStatus.PENDING));

        assertThatThrownBy(() -> guideService.updateGuide(1L, 10L,
                new UpdateGuideRequest("new title", null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(guideMapper, never()).updateById(any(Guide.class));
    }

    @Test
    @DisplayName("空更新：不改变状态")
    void updateGuide_noContentFields_shouldKeepStatus() {
        when(guideMapper.selectById(1L))
                .thenReturn(guide(1L, 10L, GuideStatus.DRAFT));

        GuideResponse response = guideService.updateGuide(1L, 10L,
                new UpdateGuideRequest(null, null, null, null, null));

        assertThat(response.status()).isEqualTo("DRAFT");
        verify(guideMapper, never()).updateById(any(Guide.class));
    }

    @Test
    @DisplayName("我的攻略：按作者与状态查询并映射驳回理由")
    void listMyGuides_shouldFilterByAuthorAndStatus() {
        Guide rejected = guide(1L, 10L, GuideStatus.REJECTED);
        rejected.setRejectReason("内容不合规");
        Guide draft = guide(2L, 10L, GuideStatus.DRAFT);
        when(guideMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Guide> page = invocation.getArgument(0);
            page.setRecords(List.of(rejected, draft));
            page.setTotal(2L);
            return page;
        });
        when(destinationMapper.selectBatchIds(any())).thenReturn(List.of(
                Destination.builder().id(3L).name("杭州").build()));
        when(commentMapper.selectList(any())).thenReturn(List.of(
                Comment.builder().id(1L).guideId(1L).content("c").build()));

        Page<MyGuideListResponse> result = guideService.listMyGuides(10L, 1, 10, null);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).status()).isEqualTo("REJECTED");
        assertThat(result.getRecords().get(0).rejectReason()).isEqualTo("内容不合规");
        assertThat(result.getRecords().get(0).commentCount()).isEqualTo(1);
        assertThat(result.getRecords().get(1).status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("作者管理详情：非作者拒绝")
    void getManageDetail_nonOwner_shouldThrow() {
        when(guideMapper.selectById(1L))
                .thenReturn(guide(1L, 2L, GuideStatus.DRAFT));

        assertThatThrownBy(() -> guideService.getManageDetail(1L, 1L))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("作者管理详情：PENDING 也可读取且绕过缓存")
    void getManageDetail_pendingOwner_shouldReturnContentAndReason() {
        Guide pending = guide(1L, 10L, GuideStatus.PENDING);
        pending.setRejectReason("待修改");
        when(guideMapper.selectById(1L)).thenReturn(pending);

        GuideResponse response = guideService.getManageDetail(1L, 10L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.content()).isEqualTo("<p>content</p>");
        assertThat(response.rejectReason()).isEqualTo("待修改");
        verifyNoInteractions(cacheService);
        verifyNoInteractions(guideBloomFilter);
    }
}
