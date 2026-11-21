package com.ctrip.operation.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.operation.dto.BannerRequest;
import com.ctrip.operation.dto.BannerResponse;
import com.ctrip.operation.entity.Banner;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.mapper.BannerMapper;
import com.ctrip.operation.service.impl.BannerServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("轮播图服务单元测试")
class BannerServiceImplTest {

    @Mock
    private BannerMapper bannerMapper;

    private BannerServiceImpl bannerService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Banner.class);
    }

    @BeforeEach
    void setUp() {
        bannerService = new BannerServiceImpl(bannerMapper);
    }

    private Banner banner(Long id, DisplayStatus status, Integer sort,
                          LocalDateTime startAt, LocalDateTime endAt) {
        return Banner.builder()
                .id(id)
                .title("标题" + id)
                .imageUrl("https://img.example.com/" + id + ".jpg")
                .linkUrl("/destinations")
                .sortOrder(sort)
                .status(status)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    @Test
    @DisplayName("创建：状态默认启用、排序默认 0")
    void create_shouldDefaultEnabledAndSortZero() {
        BannerRequest request = new BannerRequest(
                "首页轮播", "https://img.example.com/banner.jpg",
                "/destinations", null, null, null, null);

        bannerService.create(request);

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DisplayStatus.ENABLED);
        assertThat(captor.getValue().getSortOrder()).isZero();
    }

    @Test
    @DisplayName("创建：非法时间窗拒绝")
    void create_invalidWindow_shouldThrow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        BannerRequest request = new BannerRequest(
                "首页轮播", "https://img.example.com/banner.jpg",
                null, 0, DisplayStatus.ENABLED, now, now);

        assertThatThrownBy(() -> bannerService.create(request))
                .isInstanceOf(BusinessException.class);
        verify(bannerMapper, never()).insert(any(Banner.class));
    }

    @Test
    @DisplayName("更新：非法时间窗拒绝")
    void update_invalidWindow_shouldThrow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(bannerMapper.selectById(1L)).thenReturn(banner(
                1L, DisplayStatus.ENABLED, 0, null, null));
        BannerRequest request = new BannerRequest(
                "首页轮播", "https://img.example.com/banner.jpg",
                null, 0, DisplayStatus.ENABLED, now.plusDays(1), now);

        assertThatThrownBy(() -> bannerService.update(1L, request))
                .isInstanceOf(BusinessException.class);
        verify(bannerMapper, never()).updateById(any(Banner.class));
    }

    @Test
    @DisplayName("公开列表：仅返回启用且时间窗内的记录，按排序升序")
    void listActive_shouldFilterDisabledAndOutsideWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        List<Banner> banners = List.of(
                banner(3L, DisplayStatus.ENABLED, 0, now.plusHours(1), null),
                banner(2L, DisplayStatus.DISABLED, 1, null, null),
                banner(5L, DisplayStatus.ENABLED, 4, null, now.minusHours(1)),
                banner(4L, DisplayStatus.ENABLED, 3, now.minusHours(1), now.plusHours(1)),
                banner(1L, DisplayStatus.ENABLED, 2, null, null)
        );
        when(bannerMapper.selectList(any())).thenReturn(banners);

        List<BannerResponse> result = bannerService.listActiveBanners(now);

        assertThat(result).extracting(BannerResponse::id).containsExactly(1L, 4L);
    }

    @Test
    @DisplayName("公开列表：无可用数据返回空列表")
    void listActive_empty_shouldReturnEmptyList() {
        when(bannerMapper.selectList(any())).thenReturn(List.of());

        assertThat(bannerService.listActiveBanners(LocalDateTime.now())).isEmpty();
    }
}
