package com.ctrip.operation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 平台数据概览响应。
 */
public record DashboardResponse(
        long totalUsers,
        long todayNewUsers,
        long totalGuides,
        long pendingGuides,
        long publishedGuides,
        long totalOrders,
        long pendingOrders,
        long paidOrders,
        long processingOrders,
        long openedOrders,
        long refundedOrders,
        long cancelledOrders,
        BigDecimal totalSales,
        List<DailyCount> last7Days,
        long bannerCount,
        long recommendationCount
) {

    public record DailyCount(LocalDate date, long newUsers, long newOrders) {
    }
}
