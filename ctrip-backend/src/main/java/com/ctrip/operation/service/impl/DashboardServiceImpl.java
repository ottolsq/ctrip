package com.ctrip.operation.service.impl;

import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.operation.dto.DashboardResponse;
import com.ctrip.operation.dto.DashboardResponse.DailyCount;
import com.ctrip.operation.mapper.BannerMapper;
import com.ctrip.operation.mapper.RecommendationMapper;
import com.ctrip.operation.service.DashboardService;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台数据概览实现：小数据量下使用 Java 聚合，不引入数仓。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final GuideMapper guideMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final BannerMapper bannerMapper;
    private final RecommendationMapper recommendationMapper;

    public DashboardServiceImpl(UserMapper userMapper,
                                GuideMapper guideMapper,
                                BlindBoxOrderMapper orderMapper,
                                BannerMapper bannerMapper,
                                RecommendationMapper recommendationMapper) {
        this.userMapper = userMapper;
        this.guideMapper = guideMapper;
        this.orderMapper = orderMapper;
        this.bannerMapper = bannerMapper;
        this.recommendationMapper = recommendationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        List<User> users = userMapper.selectList(null);
        List<Guide> guides = guideMapper.selectList(null);
        List<BlindBoxOrder> orders = orderMapper.selectList(null);

        long todayNewUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null
                        && user.getCreatedAt().toLocalDate().equals(today))
                .count();
        long pendingGuides = countGuides(guides, GuideStatus.PENDING);
        long publishedGuides = countGuides(guides, GuideStatus.PUBLISHED);

        long pendingOrders = countOrders(orders, OrderStatus.PENDING);
        long paidOrders = countOrders(orders, OrderStatus.PAID);
        long processingOrders = countOrders(orders, OrderStatus.PROCESSING);
        long openedOrders = countOrders(orders, OrderStatus.OPENED);
        long refundedOrders = countOrders(orders, OrderStatus.REFUNDED);
        long cancelledOrders = countOrders(orders, OrderStatus.CANCELLED);

        BigDecimal totalSales = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.PROCESSING
                        || order.getStatus() == OrderStatus.OPENED)
                .map(BlindBoxOrder::getPayAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<LocalDate, Long> usersByDay = new HashMap<>();
        for (User user : users) {
            if (user.getCreatedAt() == null) {
                continue;
            }
            LocalDate date = user.getCreatedAt().toLocalDate();
            if (!date.isBefore(start) && !date.isAfter(today)) {
                usersByDay.merge(date, 1L, Long::sum);
            }
        }
        Map<LocalDate, Long> ordersByDay = new HashMap<>();
        for (BlindBoxOrder order : orders) {
            if (order.getCreatedAt() == null) {
                continue;
            }
            LocalDate date = order.getCreatedAt().toLocalDate();
            if (!date.isBefore(start) && !date.isAfter(today)) {
                ordersByDay.merge(date, 1L, Long::sum);
            }
        }
        List<DailyCount> last7Days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            last7Days.add(new DailyCount(
                    date,
                    usersByDay.getOrDefault(date, 0L),
                    ordersByDay.getOrDefault(date, 0L)));
        }

        long bannerCount = bannerMapper.selectCount(null);
        long recommendationCount = recommendationMapper.selectCount(null);

        return new DashboardResponse(
                users.size(), todayNewUsers,
                guides.size(), pendingGuides, publishedGuides,
                orders.size(), pendingOrders, paidOrders, processingOrders,
                openedOrders, refundedOrders, cancelledOrders,
                totalSales, last7Days, bannerCount, recommendationCount);
    }

    private long countGuides(List<Guide> guides, GuideStatus status) {
        return guides.stream().filter(guide -> guide.getStatus() == status).count();
    }

    private long countOrders(List<BlindBoxOrder> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }
}
