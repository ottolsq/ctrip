package com.ctrip.content.service;

import com.ctrip.content.dto.response.AdminContentStatsResponse;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.impl.AdminContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("内容数据统计单元测试")
class AdminContentServiceImplTest {

    @Mock
    private GuideMapper guideMapper;
    @Mock
    private CommentMapper commentMapper;

    private AdminContentServiceImpl adminContentService;

    @BeforeEach
    void setUp() {
        adminContentService = new AdminContentServiceImpl(guideMapper, commentMapper);
    }

    private Guide guide(Long id, GuideStatus status, int views, int likes, LocalDate createdDate) {
        return Guide.builder()
                .id(id)
                .authorId(10L)
                .title("guide" + id)
                .status(status)
                .viewCount(views)
                .likeCount(likes)
                .createdAt(createdDate.atTime(10, 0))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("聚合攻略状态、浏览点赞、Top5 与最近 7 天")
    void getContentStats_shouldAggregateCountsTopGuidesAndLast7Days() {
        LocalDate today = LocalDate.now();
        List<Guide> guides = new ArrayList<>(List.of(
                guide(1L, GuideStatus.PUBLISHED, 100, 10, today),
                guide(2L, GuideStatus.PUBLISHED, 80, 8, today.minusDays(1)),
                guide(3L, GuideStatus.PUBLISHED, 60, 6, today.minusDays(1)),
                guide(4L, GuideStatus.PUBLISHED, 40, 4, today.minusDays(3)),
                guide(5L, GuideStatus.PENDING, 30, 3, today.minusDays(10)),
                guide(6L, GuideStatus.PENDING, 20, 2, today.minusDays(10)),
                guide(7L, GuideStatus.DRAFT, 10, 1, today.minusDays(10)),
                guide(8L, GuideStatus.REJECTED, 5, 0, today.minusDays(10))
        ));
        when(guideMapper.selectList(isNull())).thenReturn(guides);
        when(commentMapper.selectCount(isNull())).thenReturn(12L);

        AdminContentStatsResponse stats = adminContentService.getContentStats();

        assertThat(stats.totalGuides()).isEqualTo(8);
        assertThat(stats.draftCount()).isEqualTo(1);
        assertThat(stats.pendingCount()).isEqualTo(2);
        assertThat(stats.publishedCount()).isEqualTo(4);
        assertThat(stats.rejectedCount()).isEqualTo(1);
        assertThat(stats.totalViews()).isEqualTo(345);
        assertThat(stats.totalLikes()).isEqualTo(34);
        assertThat(stats.totalComments()).isEqualTo(12);
        assertThat(stats.topGuides()).hasSize(5);
        assertThat(stats.topGuides().get(0).id()).isEqualTo(1L);
        assertThat(stats.topGuides().get(4).id()).isEqualTo(5L);
        assertThat(stats.last7Days()).hasSize(7);
        assertThat(stats.last7Days().get(6).date()).isEqualTo(today);
        assertThat(stats.last7Days().get(6).count()).isEqualTo(1);
        assertThat(stats.last7Days().get(5).count()).isEqualTo(2);
        assertThat(stats.last7Days().get(3).count()).isEqualTo(1);
        assertThat(stats.last7Days().get(0).count()).isZero();
    }
}
