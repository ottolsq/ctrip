package com.ctrip.itinerary.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.itinerary.dto.request.UpdateItineraryRequest;
import com.ctrip.itinerary.dto.response.ItineraryResponse;
import com.ctrip.itinerary.entity.Itinerary;
import com.ctrip.itinerary.entity.ItineraryDay;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import com.ctrip.itinerary.mapper.ItineraryDayMapper;
import com.ctrip.itinerary.mapper.ItineraryItemMapper;
import com.ctrip.itinerary.mapper.ItineraryMapper;
import com.ctrip.itinerary.service.impl.ItineraryServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link ItineraryServiceImpl} 状态流转单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("行程服务单元测试")
class ItineraryServiceImplTest {

    @Mock
    private ItineraryMapper itineraryMapper;
    @Mock
    private ItineraryDayMapper itineraryDayMapper;
    @Mock
    private ItineraryItemMapper itineraryItemMapper;
    @Mock
    private CollectionService collectionService;

    private ItineraryServiceImpl itineraryService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Itinerary.class);
        TableInfoHelper.initTableInfo(assistant, ItineraryDay.class);
    }

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryServiceImpl(
                itineraryMapper, itineraryDayMapper, itineraryItemMapper, collectionService);
    }

    @Test
    @DisplayName("已归档行程仅修改状态为已发布：允许并返回新状态")
    void updateItinerary_archivedStatusOnlyRepublish_shouldSucceed() {
        Itinerary archived = Itinerary.builder()
                .id(1L).userId(7L).title("上海周末")
                .status(ItineraryStatus.ARCHIVED)
                .build();
        Itinerary published = Itinerary.builder()
                .id(1L).userId(7L).title("上海周末")
                .status(ItineraryStatus.PUBLISHED)
                .build();
        when(itineraryMapper.selectById(1L)).thenReturn(archived, published);
        when(itineraryDayMapper.selectList(any())).thenReturn(List.of());

        ItineraryResponse response = itineraryService.updateItinerary(
                1L, 7L, new UpdateItineraryRequest(null, null, null, null, "PUBLISHED"));

        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.isOwner()).isTrue();
    }

    @Test
    @DisplayName("已归档行程修改内容：仍然拒绝")
    void updateItinerary_archivedContentEdit_shouldThrowForbidden() {
        Itinerary archived = Itinerary.builder()
                .id(1L).userId(7L).title("上海周末")
                .status(ItineraryStatus.ARCHIVED)
                .build();
        when(itineraryMapper.selectById(1L)).thenReturn(archived);

        assertThatThrownBy(() -> itineraryService.updateItinerary(
                1L, 7L, new UpdateItineraryRequest("新标题", null, null, null, "PUBLISHED")))
                .isInstanceOf(ForbiddenException.class);
    }
}
