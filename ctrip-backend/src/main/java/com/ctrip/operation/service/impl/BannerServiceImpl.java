package com.ctrip.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.operation.dto.BannerRequest;
import com.ctrip.operation.dto.BannerResponse;
import com.ctrip.operation.entity.Banner;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.mapper.BannerMapper;
import com.ctrip.operation.service.BannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 轮播图服务实现。
 */
@Service
public class BannerServiceImpl implements BannerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BannerMapper bannerMapper;

    public BannerServiceImpl(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BannerResponse> listBanners(int page, int size, DisplayStatus status) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Banner::getStatus, status);
        }
        wrapper.orderByAsc(Banner::getSortOrder).orderByAsc(Banner::getId);

        Page<Banner> result = bannerMapper.selectPage(new Page<>(page, effectiveSize), wrapper);
        Page<BannerResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listActiveBanners(LocalDateTime now) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, DisplayStatus.ENABLED);
        List<Banner> banners = bannerMapper.selectList(wrapper);
        return banners.stream()
                .filter(banner -> banner.getStatus() == DisplayStatus.ENABLED)
                .filter(banner -> banner.getStartAt() == null || !now.isBefore(banner.getStartAt()))
                .filter(banner -> banner.getEndAt() == null || !now.isAfter(banner.getEndAt()))
                .sorted(Comparator.comparing(
                                (Banner banner) -> banner.getSortOrder() == null ? 0 : banner.getSortOrder())
                        .thenComparing(banner -> banner.getId() == null ? 0L : banner.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse create(BannerRequest request) {
        validateWindow(request.startAt(), request.endAt());
        Banner banner = Banner.builder()
                .title(request.title())
                .imageUrl(request.imageUrl())
                .linkUrl(request.linkUrl())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .status(request.status() == null ? DisplayStatus.ENABLED : request.status())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
        bannerMapper.insert(banner);
        return toResponse(banner);
    }

    @Override
    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner existing = requireBanner(id);
        validateWindow(request.startAt(), request.endAt());

        existing.setTitle(request.title());
        existing.setImageUrl(request.imageUrl());
        existing.setLinkUrl(request.linkUrl());
        existing.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        existing.setStatus(request.status() == null ? existing.getStatus() : request.status());
        existing.setStartAt(request.startAt());
        existing.setEndAt(request.endAt());
        if (bannerMapper.updateById(existing) == 0) {
            throw new BusinessException("轮播图已被他人修改，请刷新后重试");
        }
        return toResponse(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireBanner(id);
        bannerMapper.deleteById(id);
    }

    private void validateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BusinessException("展示开始时间必须早于结束时间");
        }
    }

    private Banner requireBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new ResourceNotFoundException("轮播图不存在：id=" + id);
        }
        return banner;
    }

    private BannerResponse toResponse(Banner banner) {
        return new BannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getSortOrder(),
                banner.getStatus() != null ? banner.getStatus().name() : null,
                banner.getStartAt(),
                banner.getEndAt(),
                banner.getCreatedAt()
        );
    }
}
