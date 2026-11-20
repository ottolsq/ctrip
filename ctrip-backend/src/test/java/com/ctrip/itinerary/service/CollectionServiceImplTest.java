package com.ctrip.itinerary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.entity.Guide;
import com.ctrip.content.entity.enums.GuideStatus;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.mapper.GuideMapper;
import com.ctrip.itinerary.dto.response.CollectionItemResponse;
import com.ctrip.itinerary.entity.Collection;
import com.ctrip.itinerary.entity.Itinerary;
import com.ctrip.itinerary.entity.enums.CollectionTargetType;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import com.ctrip.itinerary.mapper.CollectionMapper;
import com.ctrip.itinerary.mapper.ItineraryMapper;
import com.ctrip.itinerary.service.impl.CollectionServiceImpl;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CollectionServiceImpl} 单元测试。
 *
 * <p>不启动 Spring 容器，使用 Mockito 模拟各 Mapper。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("收藏服务单元测试")
class CollectionServiceImplTest {

    @Mock
    private CollectionMapper collectionMapper;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private ItineraryMapper itineraryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DestinationMapper destinationMapper;

    private CollectionServiceImpl collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new CollectionServiceImpl(
                collectionMapper, guideMapper, itineraryMapper, userMapper, destinationMapper);
    }

    @Test
    @DisplayName("收藏已发布攻略：写入收藏记录")
    void collect_guidePublished_shouldInsertCollection() {
        Guide guide = Guide.builder().id(10L).status(GuideStatus.PUBLISHED).build();
        when(guideMapper.selectById(10L)).thenReturn(guide);
        when(collectionMapper.selectCount(any())).thenReturn(0L);

        collectionService.collect(1L, CollectionTargetType.GUIDE, 10L);

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(collectionMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getTargetType()).isEqualTo(CollectionTargetType.GUIDE);
        assertThat(captor.getValue().getTargetId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("收藏未发布攻略：抛出业务异常且不写入")
    void collect_guideNotPublished_shouldThrowBusinessException() {
        Guide guide = Guide.builder().id(10L).status(GuideStatus.DRAFT).build();
        when(guideMapper.selectById(10L)).thenReturn(guide);

        assertThatThrownBy(() -> collectionService.collect(1L, CollectionTargetType.GUIDE, 10L))
                .isInstanceOf(BusinessException.class);

        verify(collectionMapper, never()).insert(any(Collection.class));
    }

    @Test
    @DisplayName("重复收藏：不再插入第二条记录")
    void collect_duplicate_shouldNotInsertAgain() {
        Guide guide = Guide.builder().id(10L).status(GuideStatus.PUBLISHED).build();
        when(guideMapper.selectById(10L)).thenReturn(guide);
        when(collectionMapper.selectCount(any())).thenReturn(1L);

        collectionService.collect(1L, CollectionTargetType.GUIDE, 10L);

        verify(collectionMapper, never()).insert(any(Collection.class));
    }

    @Test
    @DisplayName("取消收藏：按用户与目标删除")
    void uncollect_shouldDeleteMatchingCollection() {
        collectionService.uncollect(1L, CollectionTargetType.GUIDE, 10L);

        verify(collectionMapper).delete(any());
    }

    @Test
    @DisplayName("收藏状态查询：存在记录返回 true")
    void isCollected_whenExists_shouldReturnTrue() {
        when(collectionMapper.selectCount(any())).thenReturn(1L);

        assertThat(collectionService.isCollected(1L, CollectionTargetType.GUIDE, 10L)).isTrue();
    }

    @Test
    @DisplayName("收藏列表：装配攻略与行程信息")
    void listCollections_shouldReturnEnrichedGuideAndItinerary() {
        Collection guideCollection = Collection.builder()
                .id(100L).userId(1L)
                .targetType(CollectionTargetType.GUIDE).targetId(10L)
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();
        Collection itineraryCollection = Collection.builder()
                .id(101L).userId(1L)
                .targetType(CollectionTargetType.ITINERARY).targetId(20L)
                .createdAt(LocalDateTime.of(2026, 11, 2, 10, 0))
                .build();
        when(collectionMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Collection> page = invocation.getArgument(0);
            page.setRecords(List.of(guideCollection, itineraryCollection));
            page.setTotal(2L);
            return page;
        });

        Guide guide = Guide.builder()
                .id(10L).authorId(2L).title("杭州三日游")
                .content("<p>西湖<strong>攻略</strong>正文</p>")
                .coverUrl("https://img.example.com/hangzhou.jpg")
                .status(GuideStatus.PUBLISHED)
                .build();
        when(guideMapper.selectBatchIds(any())).thenReturn(List.of(guide));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(User.builder().id(2L).username("Alice").build()));

        Itinerary itinerary = Itinerary.builder()
                .id(20L).userId(1L).title("上海周末")
                .destinationId(30L)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 3))
                .status(ItineraryStatus.PUBLISHED)
                .build();
        when(itineraryMapper.selectBatchIds(any())).thenReturn(List.of(itinerary));
        when(destinationMapper.selectBatchIds(any()))
                .thenReturn(List.of(Destination.builder().id(30L).name("上海").build()));

        Page<CollectionItemResponse> result = collectionService.listCollections(1L, 1, 10, null);

        assertThat(result.getRecords()).hasSize(2);

        CollectionItemResponse guideItem = result.getRecords().get(0);
        assertThat(guideItem.id()).isEqualTo(100L);
        assertThat(guideItem.contentType()).isEqualTo("GUIDE");
        assertThat(guideItem.contentId()).isEqualTo(10L);
        assertThat(guideItem.title()).isEqualTo("杭州三日游");
        assertThat(guideItem.coverUrl()).isEqualTo("https://img.example.com/hangzhou.jpg");
        assertThat(guideItem.summary()).isEqualTo("西湖攻略正文");
        assertThat(guideItem.authorName()).isEqualTo("Alice");

        CollectionItemResponse itineraryItem = result.getRecords().get(1);
        assertThat(itineraryItem.contentType()).isEqualTo("ITINERARY");
        assertThat(itineraryItem.contentId()).isEqualTo(20L);
        assertThat(itineraryItem.title()).isEqualTo("上海周末");
        assertThat(itineraryItem.summary()).isEqualTo("上海 · 2026-12-01 ~ 2026-12-03");
        assertThat(itineraryItem.authorName()).isNull();
    }

    @Test
    @DisplayName("收藏目标已删除：跳过该条并清理孤立收藏")
    void listCollections_shouldSkipDeletedTargets() {
        Collection orphan = Collection.builder()
                .id(100L).userId(1L)
                .targetType(CollectionTargetType.GUIDE).targetId(999L)
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();
        when(collectionMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Collection> page = invocation.getArgument(0);
            page.setRecords(List.of(orphan));
            page.setTotal(1L);
            return page;
        });
        when(guideMapper.selectBatchIds(any())).thenReturn(List.of());

        Page<CollectionItemResponse> result = collectionService.listCollections(1L, 1, 10, null);

        assertThat(result.getRecords()).isEmpty();
        verify(collectionMapper).deleteBatchIds(argThat(ids -> ids.contains(100L)));
    }
}
