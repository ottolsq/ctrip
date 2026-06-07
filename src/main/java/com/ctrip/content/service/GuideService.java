package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.request.CreateGuideRequest;
import com.ctrip.content.dto.request.UpdateGuideRequest;
import com.ctrip.content.dto.response.GuideListResponse;
import com.ctrip.content.dto.response.GuideResponse;

/**
 * 攻略服务接口，负责攻略的发布、浏览、编辑和互动。
 */
public interface GuideService {

    Page<GuideListResponse> listGuides(int page, int limit, Long destinationId, Long authorId, String keyword, String sortBy);

    GuideResponse getDetail(Long id);

    GuideResponse createGuide(Long authorId, CreateGuideRequest request);

    GuideResponse updateGuide(Long id, Long userId, UpdateGuideRequest request);

    void deleteGuide(Long id, Long userId);

    void likeGuide(Long id, Long userId);

    void unlikeGuide(Long id, Long userId);
}
