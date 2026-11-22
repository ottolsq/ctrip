package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.service.impl.BlindBoxItineraryImportServiceImpl;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.itinerary.dto.request.CreateItineraryItemRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryRequest;
import com.ctrip.itinerary.dto.response.ItineraryDayResponse;
import com.ctrip.itinerary.dto.response.ItineraryItemResponse;
import com.ctrip.itinerary.dto.response.ItineraryResponse;
import com.ctrip.itinerary.entity.enums.ItemType;
import com.ctrip.itinerary.service.ItineraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("盲盒结果导入行程服务单元测试")
class BlindBoxItineraryImportServiceImplTest {

    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxResultMapper resultMapper;
    @Mock
    private ItineraryService itineraryService;
    @Mock
    private RedissonClient redissonClient;

    private BlindBoxItineraryImportServiceImpl importService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxResult.class);
    }

    @BeforeEach
    void setUp() {
        importService = new BlindBoxItineraryImportServiceImpl(
                orderMapper, resultMapper, itineraryService, redissonClient, new ObjectMapper());
    }

    private BlindBoxOrder order(OrderStatus status, Long userId) {
        return BlindBoxOrder.builder()
                .id(10L).userId(userId).templateId(2L)
                .orderNo("BB202611220001").status(status)
                .build();
    }

    private BlindBoxResult result(String resultText, Long importedItineraryId) {
        return BlindBoxResult.builder()
                .id(100L).orderId(10L)
                .destination("杭州").destinationId(3L).theme("文化")
                .resultText(resultText)
                .importedItineraryId(importedItineraryId)
                .build();
    }

    private String validResultText() {
        return """
                {"destination":"杭州","destinationId":3,"theme":"文化","days":2,
                 "itinerary":{"day1":{"title":"西湖","items":["上午游览西湖","午餐品尝杭帮菜"]}},
                 "hotel":{"name":"杭州酒店","address":"市中心","rating":4.5},
                 "transport":{"type":"高铁","departureTime":"08:00","description":"上海到杭州"}}
                """;
    }

    private ItineraryResponse itineraryResponse(Long id) {
        return new ItineraryResponse(
                id, true, "杭州·文化·2天", 3L,
                LocalDate.now(), LocalDate.now().plusDays(1), "DRAFT",
                null, 0, 0, null, null,
                List.of(
                        new ItineraryDayResponse(11L, 1, "Day 1", 1, List.of()),
                        new ItineraryDayResponse(12L, 2, "Day 2", 2, List.of())));
    }

    private void mockLock() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("正常导入：创建行程、行程项、交通与酒店并写回结果")
    void import_openedResult_shouldCreateItineraryWithDaysItemsHotelTransport() throws Exception {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result(validResultText(), null));
        mockLock();
        when(itineraryService.createItinerary(eq(1L), any(CreateItineraryRequest.class)))
                .thenReturn(itineraryResponse(99L));
        when(itineraryService.getDetail(99L, 1L)).thenReturn(itineraryResponse(99L));
        when(itineraryService.addItem(anyLong(), anyLong(), eq(1L), any()))
                .thenReturn(new ItineraryItemResponse(
                        1L, 11L, "ATTRACTION", "西湖", "", "上午", "", 1));
        when(resultMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ItineraryResponse response = importService.importItinerary(1L, "BB202611220001");

        assertThat(response.id()).isEqualTo(99L);
        verify(itineraryService).createItinerary(eq(1L), any(CreateItineraryRequest.class));
        ArgumentCaptor<CreateItineraryItemRequest> captor =
                ArgumentCaptor.forClass(CreateItineraryItemRequest.class);
        verify(itineraryService, org.mockito.Mockito.times(4))
                .addItem(anyLong(), anyLong(), eq(1L), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(CreateItineraryItemRequest::type)
                .containsExactly(ItemType.ATTRACTION, ItemType.FOOD,
                        ItemType.TRANSPORT, ItemType.HOTEL);

        ArgumentCaptor<LambdaUpdateWrapper<BlindBoxResult>> updateCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(resultMapper).update(eq(null), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSqlSet()).contains("imported_itinerary_id");
    }

    @Test
    @DisplayName("已导入：直接返回原行程，不重复创建")
    void import_alreadyImported_shouldReturnExisting() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result(validResultText(), 50L));
        when(itineraryService.getDetail(50L, 1L)).thenReturn(itineraryResponse(50L));

        ItineraryResponse response = importService.importItinerary(1L, "BB202611220001");

        assertThat(response.id()).isEqualTo(50L);
        verify(itineraryService, never()).createItinerary(any(), any());
    }

    @Test
    @DisplayName("未开盒订单拒绝")
    void import_notOpened_shouldThrow() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.PAID, 1L));

        assertThatThrownBy(() -> importService.importItinerary(1L, "BB202611220001"))
                .isInstanceOf(ForbiddenException.class);
        verify(resultMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("非订单所有者拒绝")
    void import_otherUser_shouldThrow() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 2L));

        assertThatThrownBy(() -> importService.importItinerary(1L, "BB202611220001"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("结果 JSON 非法：回滚且不创建行程")
    void import_invalidResultText_shouldThrow() throws Exception {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result("{bad", null));
        mockLock();

        assertThatThrownBy(() -> importService.importItinerary(1L, "BB202611220001"))
                .isInstanceOf(BusinessException.class);
        verify(itineraryService, never()).createItinerary(any(), any());
        verify(resultMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("itinerary 为空：仍生成天空日程")
    void import_emptyItinerary_shouldStillCreateEmptyDays() throws Exception {
        String text = """
                {"destination":"杭州","destinationId":3,"theme":"文化","days":2,"itinerary":{}}
                """;
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result(text, null));
        mockLock();
        when(itineraryService.createItinerary(eq(1L), any(CreateItineraryRequest.class)))
                .thenReturn(itineraryResponse(99L));
        when(itineraryService.getDetail(99L, 1L)).thenReturn(itineraryResponse(99L));
        when(resultMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ItineraryResponse response = importService.importItinerary(1L, "BB202611220001");

        assertThat(response.id()).isEqualTo(99L);
        verify(itineraryService, never()).addItem(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("条件更新 0 行：视为并发冲突并抛业务异常")
    void import_conditionalUpdateZeroRows_shouldThrow() throws Exception {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result(validResultText(), null));
        mockLock();
        when(itineraryService.createItinerary(eq(1L), any(CreateItineraryRequest.class)))
                .thenReturn(itineraryResponse(99L));
        when(itineraryService.addItem(anyLong(), anyLong(), eq(1L), any()))
                .thenReturn(new ItineraryItemResponse(
                        1L, 11L, "ATTRACTION", "西湖", "", "上午", "", 1));
        when(resultMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> importService.importItinerary(1L, "BB202611220001"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("超长字段与超大天数：按数据库/业务上限截断")
    void import_longFieldsAndDays_shouldBeLimited() throws Exception {
        String longText = "很长".repeat(100);
        String text = """
                {"destination":"%s","destinationId":3,"theme":"文化","days":100,
                 "itinerary":{"day1":{"items":[
                   "%s",
                   {"name":"%s","location":"%s","timeSlot":"%s","type":"ACTIVITY"}
                 ]}}}
                """.formatted(longText, longText, longText, longText, longText);
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.OPENED, 1L));
        when(resultMapper.selectOne(any())).thenReturn(result(text, null));
        mockLock();
        when(itineraryService.createItinerary(eq(1L), any(CreateItineraryRequest.class)))
                .thenReturn(itineraryResponse(99L));
        when(itineraryService.getDetail(99L, 1L)).thenReturn(itineraryResponse(99L));
        when(itineraryService.addItem(anyLong(), anyLong(), eq(1L), any()))
                .thenReturn(new ItineraryItemResponse(
                        1L, 11L, "ATTRACTION", "西湖", "", "上午", "", 1));
        when(resultMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        importService.importItinerary(1L, "BB202611220001");

        ArgumentCaptor<CreateItineraryRequest> createCaptor =
                ArgumentCaptor.forClass(CreateItineraryRequest.class);
        verify(itineraryService).createItinerary(eq(1L), createCaptor.capture());
        assertThat(createCaptor.getValue().title().length()).isLessThanOrEqualTo(100);
        assertThat(java.time.temporal.ChronoUnit.DAYS.between(
                createCaptor.getValue().startDate(), createCaptor.getValue().endDate()))
                .isEqualTo(29);

        ArgumentCaptor<CreateItineraryItemRequest> itemCaptor =
                ArgumentCaptor.forClass(CreateItineraryItemRequest.class);
        verify(itineraryService, org.mockito.Mockito.atLeastOnce())
                .addItem(anyLong(), anyLong(), eq(1L), itemCaptor.capture());
        for (CreateItineraryItemRequest item : itemCaptor.getAllValues()) {
            assertThat(item.name().length()).isLessThanOrEqualTo(30);
            if (item.location() != null) {
                assertThat(item.location().length()).isLessThanOrEqualTo(255);
            }
            if (item.timeSlot() != null) {
                assertThat(item.timeSlot().length()).isLessThanOrEqualTo(50);
            }
        }
    }
}
