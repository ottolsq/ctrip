package com.ctrip.content.service.impl;

import com.ctrip.content.dto.response.AdminContentStatsResponse;
import com.ctrip.content.dto.response.AdminContentStatsResponse.DailyGuideCount;
import com.ctrip.content.dto.response.AdminContentStatsResponse.TopGuideStat;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.CommentMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.content.service.AdminContentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容运营数据统计实现。
 */
@Service
public class AdminContentServiceImpl implements AdminContentService {

    private final GuideMapper guideMapper;
    private final CommentMapper commentMapper;

    public AdminContentServiceImpl(GuideMapper guideMapper, CommentMapper commentMapper) {
        this.guideMapper = guideMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContentStatsResponse getContentStats() {
        List<Guide> guides = guideMapper.selectList(null);

        long draftCount = countByStatus(guides, GuideStatus.DRAFT);
        long pendingCount = countByStatus(guides, GuideStatus.PENDING);
        long publishedCount = countByStatus(guides, GuideStatus.PUBLISHED);
        long rejectedCount = countByStatus(guides, GuideStatus.REJECTED);
        long totalViews = guides.stream()
                .mapToLong(guide -> guide.getViewCount() == null ? 0 : guide.getViewCount())
                .sum();
        long totalLikes = guides.stream()
                .mapToLong(guide -> guide.getLikeCount() == null ? 0 : guide.getLikeCount())
                .sum();

        List<TopGuideStat> topGuides = guides.stream()
                .sorted(Comparator.comparingInt(
                        (Guide guide) -> guide.getViewCount() == null ? 0 : guide.getViewCount())
                        .reversed())
                .limit(5)
                .map(guide -> new TopGuideStat(
                        guide.getId(), guide.getTitle(),
                        guide.getViewCount(), guide.getLikeCount()))
                .toList();

        long totalComments = commentMapper.selectCount(null);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Long> dailyCounts = new HashMap<>();
        for (Guide guide : guides) {
            if (guide.getCreatedAt() == null) {
                continue;
            }
            LocalDate date = guide.getCreatedAt().toLocalDate();
            if (!date.isBefore(start) && !date.isAfter(today)) {
                dailyCounts.merge(date, 1L, Long::sum);
            }
        }
        List<DailyGuideCount> last7Days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            last7Days.add(new DailyGuideCount(date, dailyCounts.getOrDefault(date, 0L)));
        }

        return new AdminContentStatsResponse(
                guides.size(), draftCount, pendingCount, publishedCount, rejectedCount,
                totalViews, totalLikes, totalComments, topGuides, last7Days);
    }

    private long countByStatus(List<Guide> guides, GuideStatus status) {
        return guides.stream().filter(guide -> guide.getStatus() == status).count();
    }
}
