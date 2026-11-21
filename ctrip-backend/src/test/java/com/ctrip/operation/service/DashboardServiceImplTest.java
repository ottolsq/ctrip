package com.ctrip.operation.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.operation.dto.DashboardResponse;
import com.ctrip.operation.entity.Banner;
import com.ctrip.operation.entity.Recommendation;
import com.ctrip.operation.mapper.BannerMapper;
import com.ctrip.operation.mapper.RecommendationMapper;
import com.ctrip.operation.service.impl.DashboardServiceImpl;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("平台数据概览服务单元测试")
class DashboardServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BannerMapper bannerMapper;
    @Mock
    private RecommendationMapper recommendationMapper;

    private DashboardServiceImpl dashboardService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
        TableInfoHelper.initTableInfo(assistant, Banner.class);
        TableInfoHelper.initTableInfo(assistant, Recommendation.class);
    }

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                userMapper, guideMapper, orderMapper, bannerMapper, recommendationMapper);
    }

    private User user(Long id, LocalDateTime createdAt) {
        return User.builder().id(id).createdAt(createdAt).build();
    }

    private Guide guide(Long id, GuideStatus status) {
        return Guide.builder().id(id).status(status).build();
    }

    private BlindBoxOrder order(Long id, OrderStatus status, String amount,
                                LocalDateTime createdAt) {
        return BlindBoxOrder.builder()
                .id(id)
                .status(status)
                .payAmount(amount == null ? null : new BigDecimal(amount))
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("聚合：用户/攻略/订单状态/7天序列/运营数量")
    void getDashboard_shouldAggregateCountsStatusesAnd7Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        when(userMapper.selectList(any())).thenReturn(List.of(
                user(1L, now),
                user(2L, now.minusDays(1)),
                user(3L, now.minusDays(8))));
        when(guideMapper.selectList(any())).thenReturn(List.of(
                guide(1L, GuideStatus.DRAFT),
                guide(2L, GuideStatus.PENDING),
                guide(3L, GuideStatus.PUBLISHED)));
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, OrderStatus.PAID, "100.00", now),
                order(2L, OrderStatus.PROCESSING, "50.50", now.minusDays(1)),
                order(3L, OrderStatus.OPENED, "20.00", now.minusDays(2)),
                order(4L, OrderStatus.REFUNDED, "999.00", now.minusDays(3)),
                order(5L, OrderStatus.CANCELLED, "888.00", now.minusDays(4))));
        when(bannerMapper.selectCount(isNull())).thenReturn(2L);
        when(recommendationMapper.selectCount(isNull())).thenReturn(3L);

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.totalUsers()).isEqualTo(3);
        assertThat(response.todayNewUsers()).isEqualTo(1);
        assertThat(response.totalGuides()).isEqualTo(3);
        assertThat(response.pendingGuides()).isEqualTo(1);
        assertThat(response.publishedGuides()).isEqualTo(1);
        assertThat(response.totalOrders()).isEqualTo(5);
        assertThat(response.pendingOrders()).isZero();
        assertThat(response.paidOrders()).isEqualTo(1);
        assertThat(response.processingOrders()).isEqualTo(1);
        assertThat(response.openedOrders()).isEqualTo(1);
        assertThat(response.refundedOrders()).isEqualTo(1);
        assertThat(response.cancelledOrders()).isEqualTo(1);
        assertThat(response.totalSales()).isEqualByComparingTo("170.50");
        assertThat(response.bannerCount()).isEqualTo(2);
        assertThat(response.recommendationCount()).isEqualTo(3);

        assertThat(response.last7Days()).hasSize(7);
        DashboardResponse.DailyCount todayCount = response.last7Days().getLast();
        assertThat(todayCount.date()).isEqualTo(today);
        assertThat(todayCount.newUsers()).isEqualTo(1);
        assertThat(todayCount.newOrders()).isEqualTo(1);
        DashboardResponse.DailyCount yesterdayCount = response.last7Days().get(5);
        assertThat(yesterdayCount.date()).isEqualTo(today.minusDays(1));
        assertThat(yesterdayCount.newUsers()).isEqualTo(1);
        assertThat(yesterdayCount.newOrders()).isEqualTo(1);
    }

    @Test
    @DisplayName("销售额：排除退款/取消与 null 金额")
    void totalSales_shouldExcludeRefundedCancelledAndNull() {
        LocalDateTime now = LocalDateTime.now();
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(guideMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, OrderStatus.PAID, null, now),
                order(2L, OrderStatus.PAID, "100.00", now),
                order(3L, OrderStatus.PROCESSING, "50.50", now),
                order(4L, OrderStatus.OPENED, "20.00", now),
                order(5L, OrderStatus.REFUNDED, "999.00", now),
                order(6L, OrderStatus.CANCELLED, "888.00", now)));
        when(bannerMapper.selectCount(isNull())).thenReturn(0L);
        when(recommendationMapper.selectCount(isNull())).thenReturn(0L);

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.totalSales()).isEqualByComparingTo("170.50");
    }
}
