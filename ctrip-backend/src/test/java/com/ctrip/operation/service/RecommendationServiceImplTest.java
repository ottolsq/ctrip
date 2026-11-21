package com.ctrip.operation.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.operation.dto.PublicRecommendationResponse;
import com.ctrip.operation.dto.RecommendationRequest;
import com.ctrip.operation.entity.Recommendation;
import com.ctrip.operation.entity.enums.DisplayStatus;
import com.ctrip.operation.entity.enums.RecommendationTargetType;
import com.ctrip.operation.mapper.RecommendationMapper;
import com.ctrip.operation.service.impl.RecommendationServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("推荐位服务单元测试")
class RecommendationServiceImplTest {

    @Mock
    private RecommendationMapper recommendationMapper;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private DestinationMapper destinationMapper;
    @Mock
    private BlindBoxTemplateMapper templateMapper;

    private RecommendationServiceImpl recommendationService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Recommendation.class);
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, Destination.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxTemplate.class);
    }

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                recommendationMapper, guideMapper, destinationMapper, templateMapper);
    }

    private Recommendation recommendation(Long id, RecommendationTargetType type, Long targetId,
                                          int sort, DisplayStatus status,
                                          LocalDateTime startAt, LocalDateTime endAt) {
        return Recommendation.builder()
                .id(id)
                .slot("home_featured")
                .title("推荐" + id)
                .targetType(type)
                .targetId(targetId)
                .sortOrder(sort)
                .status(status)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    private RecommendationRequest request(RecommendationTargetType type, Long targetId,
                                          LocalDateTime startAt, LocalDateTime endAt) {
        return new RecommendationRequest(
                "home_featured", "首页推荐", type, targetId,
                0, DisplayStatus.ENABLED, startAt, endAt);
    }

    @Test
    @DisplayName("创建：目标不存在拒绝")
    void create_withMissingTarget_shouldThrow() {
        when(guideMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> recommendationService.create(
                request(RecommendationTargetType.GUIDE, 99L, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(recommendationMapper, never()).insert(any(Recommendation.class));
    }

    @Test
    @DisplayName("创建：非法时间窗拒绝")
    void create_invalidWindow_shouldThrow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);

        assertThatThrownBy(() -> recommendationService.create(
                request(RecommendationTargetType.DESTINATION, 1L, now, now)))
                .isInstanceOf(BusinessException.class);
        verify(recommendationMapper, never()).insert(any(Recommendation.class));
    }

    @Test
    @DisplayName("公开列表：跳过未发布攻略与不存在的模板")
    void listActive_shouldSkipUnpublishedGuideAndMissingTemplate() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(recommendationMapper.selectList(any())).thenReturn(List.of(
                recommendation(1L, RecommendationTargetType.GUIDE, 10L, 0,
                        DisplayStatus.ENABLED, null, null),
                recommendation(2L, RecommendationTargetType.GUIDE, 11L, 1,
                        DisplayStatus.ENABLED, null, null),
                recommendation(3L, RecommendationTargetType.BLIND_BOX_TEMPLATE, 20L, 2,
                        DisplayStatus.ENABLED, null, null)));
        when(guideMapper.selectById(10L)).thenReturn(Guide.builder()
                .id(10L).title("杭州三日游").coverUrl("https://img.example.com/guide.jpg")
                .status(GuideStatus.PUBLISHED).build());
        when(guideMapper.selectById(11L)).thenReturn(Guide.builder()
                .id(11L).title("审核中的攻略").status(GuideStatus.PENDING).build());
        when(templateMapper.selectById(20L)).thenReturn(null);

        List<PublicRecommendationResponse> result =
                recommendationService.listActive("home_featured", now);

        assertThat(result).hasSize(1);
        PublicRecommendationResponse item = result.getFirst();
        assertThat(item.targetTitle()).isEqualTo("杭州三日游");
        assertThat(item.coverUrl()).isEqualTo("https://img.example.com/guide.jpg");
        assertThat(item.linkUrl()).isEqualTo("/guides/10");
        assertThat(item.targetType()).isEqualTo("GUIDE");
    }

    @Test
    @DisplayName("公开列表：目的地装配封面与链接")
    void listActive_destination_shouldAssembleCoverAndLink() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(recommendationMapper.selectList(any())).thenReturn(List.of(
                recommendation(4L, RecommendationTargetType.DESTINATION, 5L, 0,
                        DisplayStatus.ENABLED, null, null)));
        when(destinationMapper.selectById(5L)).thenReturn(Destination.builder()
                .id(5L).name("杭州").coverUrl("https://img.example.com/hangzhou.jpg").build());

        List<PublicRecommendationResponse> result =
                recommendationService.listActive("home_featured", now);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().targetTitle()).isEqualTo("杭州");
        assertThat(result.getFirst().coverUrl()).isEqualTo("https://img.example.com/hangzhou.jpg");
        assertThat(result.getFirst().linkUrl()).isEqualTo("/destinations/5");
    }

    @Test
    @DisplayName("公开列表：已下架盲盒模板跳过")
    void listActive_inactiveTemplate_shouldBeSkipped() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(recommendationMapper.selectList(any())).thenReturn(List.of(
                recommendation(9L, RecommendationTargetType.BLIND_BOX_TEMPLATE, 21L, 0,
                        DisplayStatus.ENABLED, null, null)));
        when(templateMapper.selectById(21L)).thenReturn(BlindBoxTemplate.builder()
                .id(21L).name("已下架盲盒").status(TemplateStatus.INACTIVE).build());

        assertThat(recommendationService.listActive("home_featured", now)).isEmpty();
    }

    @Test
    @DisplayName("公开列表：已删除攻略跳过")
    void listActive_missingGuide_shouldBeSkipped() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(recommendationMapper.selectList(any())).thenReturn(List.of(
                recommendation(12L, RecommendationTargetType.GUIDE, 12L, 0,
                        DisplayStatus.ENABLED, null, null)));
        when(guideMapper.selectById(12L)).thenReturn(null);

        assertThat(recommendationService.listActive("home_featured", now)).isEmpty();
    }

    @Test
    @DisplayName("公开列表：停用与时间窗外记录被排除")
    void listActive_disabledOrOutsideWindow_shouldBeExcluded() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 21, 12, 0);
        when(recommendationMapper.selectList(any())).thenReturn(List.of(
                recommendation(6L, RecommendationTargetType.DESTINATION, 5L, 0,
                        DisplayStatus.DISABLED, null, null),
                recommendation(7L, RecommendationTargetType.DESTINATION, 5L, 1,
                        DisplayStatus.ENABLED, null, now.minusHours(1)),
                recommendation(8L, RecommendationTargetType.DESTINATION, 5L, 2,
                        DisplayStatus.ENABLED, now.minusHours(1), now.plusHours(1))));
        when(destinationMapper.selectById(5L)).thenReturn(Destination.builder()
                .id(5L).name("杭州").coverUrl("https://img.example.com/hangzhou.jpg").build());

        List<PublicRecommendationResponse> result =
                recommendationService.listActive("home_featured", now);

        assertThat(result).extracting(PublicRecommendationResponse::id)
                .containsExactly(8L);
    }
}
