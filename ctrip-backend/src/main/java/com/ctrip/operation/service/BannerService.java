package com.ctrip.operation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.operation.dto.BannerRequest;
import com.ctrip.operation.dto.BannerResponse;
import com.ctrip.operation.entity.enums.DisplayStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务。
 */
public interface BannerService {

    Page<BannerResponse> listBanners(int page, int size, DisplayStatus status);

    List<BannerResponse> listActiveBanners(LocalDateTime now);

    BannerResponse create(BannerRequest request);

    BannerResponse update(Long id, BannerRequest request);

    void delete(Long id);
}
