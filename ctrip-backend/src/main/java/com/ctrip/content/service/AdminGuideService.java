package com.ctrip.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.content.dto.response.AdminGuideListItemResponse;
import com.ctrip.content.dto.response.GuideResponse;
import com.ctrip.content.entity.enums.GuideStatus;

/**
 * 管理端攻略审核服务。
 */
public interface AdminGuideService {

    Page<AdminGuideListItemResponse> listGuides(int page, int size, GuideStatus status,
                                                String keyword, Long authorId);

    GuideResponse getDetail(Long guideId);

    void approve(Long guideId, Long operatorId);

    void reject(Long guideId, Long operatorId, String reason);
}
