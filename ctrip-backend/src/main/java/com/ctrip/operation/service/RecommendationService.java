package com.ctrip.operation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.operation.dto.PublicRecommendationResponse;
import com.ctrip.operation.dto.RecommendationRequest;
import com.ctrip.operation.dto.RecommendationResponse;
import com.ctrip.operation.entity.enums.DisplayStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐位服务。
 */
public interface RecommendationService {

    Page<RecommendationResponse> listRecommendations(int page, int size, String slot,
                                                     DisplayStatus status);

    List<PublicRecommendationResponse> listActive(String slot, LocalDateTime now);

    RecommendationResponse create(RecommendationRequest request);

    RecommendationResponse update(Long id, RecommendationRequest request);

    void delete(Long id);
}
