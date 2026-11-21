package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.AdminOrderDetailResponse;
import com.ctrip.blindbox.dto.AdminOrderResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.impl.AdminBlindBoxOrderServiceImpl;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.messaging.publisher.EventPublisher;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理端盲盒订单服务单元测试")
class AdminBlindBoxOrderServiceImplTest {

    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxTemplateMapper templateMapper;
    @Mock
    private BlindBoxResultMapper resultMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EventPublisher eventPublisher;

    private AdminBlindBoxOrderServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxTemplate.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxResult.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        service = new AdminBlindBoxOrderServiceImpl(
                orderMapper, templateMapper, resultMapper, userMapper, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private BlindBoxOrder order(OrderStatus status) {
        return BlindBoxOrder.builder()
                .id(10L)
                .userId(1L)
                .templateId(2L)
                .orderNo("BB202611210001")
                .status(status)
                .payAmount(new BigDecimal("99.00"))
                .payMethod("ALIPAY")
                .payTime(LocalDateTime.of(2026, 11, 21, 10, 0))
                .expireAt(LocalDateTime.of(2026, 11, 21, 10, 15))
                .createdAt(LocalDateTime.of(2026, 11, 21, 10, 0))
                .build();
    }

    @Test
    @DisplayName("订单列表：按条件筛选并填充用户名/模板名")
    void listOrders_shouldFilterAndEnrichUserNameAndTemplate() {
        BlindBoxOrder order = order(OrderStatus.PAID);
        when(orderMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BlindBoxOrder> page = invocation.getArgument(0);
            page.setRecords(List.of(order));
            page.setTotal(1);
            return page;
        });
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().id(1L).username("alice").build()));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(
                BlindBoxTemplate.builder()
                        .id(2L).name("周末盲盒").type(BlindBoxType.DAILY)
                        .price(new BigDecimal("99.00")).stock(-1)
                        .status(TemplateStatus.ACTIVE).build()));

        Page<AdminOrderResponse> result = service.listOrders(
                1, 10, "BB2026", 1L, OrderStatus.PAID,
                java.time.LocalDate.of(2026, 11, 1), java.time.LocalDate.of(2026, 11, 30));

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        AdminOrderResponse response = result.getRecords().getFirst();
        assertThat(response.userName()).isEqualTo("alice");
        assertThat(response.templateName()).isEqualTo("周末盲盒");
        assertThat(response.status()).isEqualTo("PAID");

        ArgumentCaptor<LambdaQueryWrapper<BlindBoxOrder>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("order_no", "user_id", "status", "created_at");
    }

    @Test
    @DisplayName("订单详情：已开盒订单包含开盒结果")
    void getOrderDetail_shouldIncludeResultWhenOpened() {
        BlindBoxOrder order = order(OrderStatus.OPENED);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(
                User.builder().id(1L).username("alice").build());
        when(templateMapper.selectById(2L)).thenReturn(
                BlindBoxTemplate.builder().id(2L).name("周末盲盒").build());
        BlindBoxResult result = BlindBoxResult.builder()
                .id(20L).orderId(10L).destination("杭州").destinationId(100L)
                .theme("古镇").resultText("{}").shareCode("ABC123")
                .openedAt(LocalDateTime.of(2026, 11, 21, 10, 1))
                .createdAt(LocalDateTime.of(2026, 11, 21, 10, 1))
                .build();
        when(resultMapper.selectOne(any())).thenReturn(result);

        AdminOrderDetailResponse detail = service.getOrderDetail("BB202611210001");

        assertThat(detail.order().userName()).isEqualTo("alice");
        assertThat(detail.order().templateName()).isEqualTo("周末盲盒");
        assertThat(detail.result()).isNotNull();
        assertThat(detail.result().destination()).isEqualTo("杭州");
        assertThat(detail.result().orderNo()).isEqualTo("BB202611210001");
    }

    @Test
    @DisplayName("退款：条件更新、同步恢复库存并提交后发布事件")
    void refund_paid_shouldSetRefundedAndPublishEventAfterCommit() {
        BlindBoxOrder order = order(OrderStatus.PAID);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(templateMapper.selectById(2L)).thenReturn(BlindBoxTemplate.builder()
                .id(2L).name("周末盲盒").type(BlindBoxType.LIMITED).stock(5).build());

        TransactionSynchronizationManager.initSynchronization();
        service.refund("BB202611210001", "用户申请退款", 99L);

        verify(eventPublisher, never()).publishOrderStatusEvent(
                anyString(), any(), anyString(), any(), any(), any(), anyString(), anyMap());
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(eventPublisher).publishOrderStatusEvent(
                eq("ORDER_REFUNDED"), eq(10L), eq("BB202611210001"), eq(1L), eq(2L),
                eq(new BigDecimal("99.00")), eq("ALIPAY"), anyMap());

        ArgumentCaptor<LambdaUpdateWrapper<BlindBoxOrder>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(orderMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<BlindBoxOrder> wrapper = captor.getValue();
        wrapper.getSqlSet();
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("用户申请退款", OrderStatus.REFUNDED)
                .anyMatch(value -> value instanceof LocalDateTime);
        assertThat(wrapper.getSqlSegment()).contains("status", "id");
        verify(templateMapper).incrementStock(2L);
    }

    @Test
    @DisplayName("退款：日常盲盒不恢复库存")
    void refund_dailyOrder_shouldNotRestoreStock() {
        BlindBoxOrder order = order(OrderStatus.PAID);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(templateMapper.selectById(2L)).thenReturn(BlindBoxTemplate.builder()
                .id(2L).name("日常盲盒").type(BlindBoxType.DAILY).stock(-1).build());

        service.refund("BB202611210001", "用户申请退款", 99L);

        verify(templateMapper, never()).incrementStock(any());
    }

    @Test
    @DisplayName("退款：事件发布失败不影响已提交退款与库存恢复")
    void refund_eventPublishFailure_shouldNotThrowAndStockRestored() {
        BlindBoxOrder order = order(OrderStatus.PAID);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(templateMapper.selectById(2L)).thenReturn(BlindBoxTemplate.builder()
                .id(2L).name("周末盲盒").type(BlindBoxType.LIMITED).stock(5).build());
        doThrow(new RuntimeException("mq down")).when(eventPublisher).publishOrderStatusEvent(
                anyString(), any(), anyString(), any(), any(), any(), anyString(), anyMap());

        TransactionSynchronizationManager.initSynchronization();
        assertThatCode(() -> {
            service.refund("BB202611210001", "用户申请退款", 99L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        }).doesNotThrowAnyException();
        verify(templateMapper).incrementStock(2L);
    }

    @Test
    @DisplayName("退款：未支付订单拒绝")
    void refund_pending_shouldThrow() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.PENDING));

        assertThatThrownBy(() -> service.refund("BB202611210001", "用户申请退款", 99L))
                .isInstanceOf(BusinessException.class);
        verify(orderMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("退款：并发修改导致条件更新 0 行时拒绝")
    void refund_conditionalUpdateZeroRows_shouldThrow() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.PAID));
        when(orderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.refund("BB202611210001", "用户申请退款", 99L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("异常重试：PROCESSING 订单重新发布开盒事件")
    void retryOpen_processing_shouldPublishOpenEvent() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.PROCESSING));

        service.retryOpen("BB202611210001");

        verify(eventPublisher).publishBlindBoxOpenEvent(
                eq("BLINDBOX_OPEN_REQUESTED"), eq(10L), eq(1L), isNull(), isNull());
    }

    @Test
    @DisplayName("异常重试：非 PROCESSING 订单拒绝")
    void retryOpen_nonProcessing_shouldThrow() {
        when(orderMapper.selectOne(any())).thenReturn(order(OrderStatus.PAID));

        assertThatThrownBy(() -> service.retryOpen("BB202611210001"))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(eventPublisher);
    }
}
