package com.ctrip.content.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 内容运营数据统计。
 */
public record AdminContentStatsResponse(
        long totalGuides,
        long draftCount,
        long pendingCount,
        long publishedCount,
        long rejectedCount,
        long totalViews,
        long totalLikes,
        long totalComments,
        List<TopGuideStat> topGuides,
        List<DailyGuideCount> last7Days
) {

    public record TopGuideStat(
            Long id,
            String title,
            Integer viewCount,
            Integer likeCount
    ) {}

    public record DailyGuideCount(
            LocalDate date,
            long count
    ) {}
}
