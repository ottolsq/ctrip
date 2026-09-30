package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxPreferenceMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.impl.BlindBoxOrderServiceImpl;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.messaging.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link BlindBoxOrderServiceImpl} 订单状态机单元测试。
 *
 * <p>纯 Mock 测试（不启动 Spring 容器）。通过 {@link #initMybatisLambdaCache()}
 * 手动初始化 MyBatis-Plus 实体 Lambda 缓存，避免 "can not find lambda cache" 错误。
 *
 * <p>覆盖合法状态跳转、幂等处理、非法操作拦截三大类共 14 个用例。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlindBoxOrderServiceImpl 订单状态机测试")
class BlindBoxOrderStateMachineTest {

    private static final Long USER_ID = 100L;
    private static final Long ORDER_ID = 1L;
    private static final Long TEMPLATE_ID = 10L;
    private static final String ORDER_NO = "BB202607230001";
    private static final BigDecimal PAY_AMOUNT = new BigDecimal("99.00");

    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxTemplateMapper templateMapper;
    @Mock
    private BlindBoxResultMapper resultMapper;
    @Mock
    private BlindBoxPreferenceMapper preferenceMapper;
    @Mock
    private BlindBoxPreferenceService preferenceService;
    @Mock
    private BlindBoxSchemeService schemeService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private FlashSaleService flashSaleService;
    @Mock
    private RLock rLock;

    private BlindBoxOrderServiceImpl service;

    /**
     * 手动初始化 MyBatis-Plus Lambda 缓存。
     *
     * <p>纯 Mock 环境下 Mapper 不会被 MyBatis-Plus 扫描处理，
     * 但 {@link BlindBoxOrderServiceImpl} 内部使用 {@link LambdaQueryWrapper}
     * 和 {@link LambdaUpdateWrapper}，依赖实体类的列名 Lambda 缓存。
     * 通过 {@link TableInfoHelper#initTableInfo} 手动构建缓存。
     */
    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "test");
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxTemplate.class);
    }

    @BeforeEach
    void setUp() {
        service = new BlindBoxOrderServiceImpl(
                orderMapper, templateMapper, resultMapper, preferenceMapper,
                preferenceService, schemeService, redissonClient,
                stringRedisTemplate, objectMapper, eventPublisher,
                flashSaleService);
    }

    // ========================================================================
    // 3.1 合法状态跳转
    // ========================================================================

    @Nested
    @DisplayName("合法状态跳转")
    class ValidTransitions {

        @Test
        @DisplayName("payCallback：PENDING → PAID，设置支付时间和方式")
        void payCallback_pendingOrder_shouldTransitionToPaid() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PENDING);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            service.payCallback(ORDER_NO, "ALIPAY");

            verify(orderMapper).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(eventPublisher).publishOrderStatusEvent(
                    eq("ORDER_PAID"), eq(ORDER_ID), eq(ORDER_NO), eq(USER_ID),
                    eq(TEMPLATE_ID), eq(PAY_AMOUNT), eq("ALIPAY"), any());
        }

        @Test
        @DisplayName("cancelOrder：PENDING → CANCELLED，限定盲盒恢复库存")
        void cancelOrder_pendingLimitedOrder_shouldTransitionToCancelledAndRestoreStock() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PENDING);
            BlindBoxTemplate template = buildTemplate(TEMPLATE_ID, BlindBoxType.LIMITED, 5);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
            when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            service.cancelOrder(USER_ID, ORDER_NO);

            verify(orderMapper).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(templateMapper).incrementStock(TEMPLATE_ID);
            verify(eventPublisher).publishOrderStatusEvent(
                    eq("ORDER_CANCELLED"), eq(ORDER_ID), eq(ORDER_NO), eq(USER_ID),
                    eq(TEMPLATE_ID), eq(PAY_AMOUNT), isNull(), isNull());
        }

        @Test
        @DisplayName("cancelOrder：日常盲盒取消不恢复库存")
        void cancelOrder_pendingDailyOrder_shouldNotRestoreStock() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PENDING);
            BlindBoxTemplate template = buildTemplate(TEMPLATE_ID, BlindBoxType.DAILY, -1);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
            when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            service.cancelOrder(USER_ID, ORDER_NO);

            verify(templateMapper, never()).incrementStock(anyLong());
        }

        @Test
        @DisplayName("openBox：PAID → PROCESSING，发送 MQ 消息")
        void openBox_paidOrder_shouldTransitionToProcessing() throws InterruptedException {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            BlindBoxTemplate template = buildTemplate(TEMPLATE_ID, BlindBoxType.LIMITED, 5);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            mockLockAcquired();
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

            service.openBox(USER_ID, ORDER_NO);

            verify(orderMapper).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(eventPublisher).publishBlindBoxOpenEvent(
                    eq("BLINDBOX_OPEN_REQUESTED"), eq(ORDER_ID), eq(USER_ID),
                    isNull(), isNull());
        }
    }

    // ========================================================================
    // 3.2 幂等处理
    // ========================================================================

    @Nested
    @DisplayName("幂等处理")
    class Idempotency {

        @Test
        @DisplayName("payCallback：已支付订单再次支付 → 幂等返回")
        void payCallback_alreadyPaid_shouldReturnIdempotently() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            service.payCallback(ORDER_NO, "ALIPAY");

            verify(orderMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("payCallback：已开盒订单再次支付 → 幂等返回")
        void payCallback_alreadyOpened_shouldReturnIdempotently() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.OPENED);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            service.payCallback(ORDER_NO, "WECHAT_PAY");

            verify(orderMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("openBox：并发场景——进入时 PAID，锁内重查已是 OPENED → 幂等返回")
        void openBox_raceConditionOpened_shouldReturnExistingResult() throws InterruptedException {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            BlindBoxOrder refreshedOrder = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.OPENED);
            BlindBoxTemplate template = buildTemplate(TEMPLATE_ID, BlindBoxType.LIMITED, 5);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            mockLockAcquired();
            when(orderMapper.selectById(ORDER_ID)).thenReturn(refreshedOrder);
            when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

            service.openBox(USER_ID, ORDER_NO);

            verify(orderMapper, never()).update(any(), any());
            verify(eventPublisher, never()).publishBlindBoxOpenEvent(
                    anyString(), anyLong(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("openBox：处理中订单再次开盒 → 返回 PROCESSING，不重复发 MQ")
        void openBox_alreadyProcessing_shouldReturnProcessingStatus() throws InterruptedException {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            BlindBoxOrder refreshedOrder = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PROCESSING);
            BlindBoxTemplate template = buildTemplate(TEMPLATE_ID, BlindBoxType.LIMITED, 5);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            mockLockAcquired();
            when(orderMapper.selectById(ORDER_ID)).thenReturn(refreshedOrder);
            when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

            service.openBox(USER_ID, ORDER_NO);

            verify(orderMapper, never()).update(any(), any());
            verify(eventPublisher, never()).publishBlindBoxOpenEvent(
                    anyString(), anyLong(), anyLong(), any(), any());
        }
    }

    // ========================================================================
    // 3.3 非法操作拦截
    // ========================================================================

    @Nested
    @DisplayName("非法操作拦截")
    class InvalidOperations {

        @Test
        @DisplayName("cancelOrder：已支付订单不可取消")
        void cancelOrder_paidOrder_shouldThrowBusinessException() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.cancelOrder(USER_ID, ORDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅待支付");
            verify(orderMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("cancelOrder：无权取消他人订单")
        void cancelOrder_otherUsersOrder_shouldThrowForbiddenException() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PENDING);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.cancelOrder(999L, ORDER_NO))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("无权");
        }

        @Test
        @DisplayName("payCallback：已取消订单不可支付")
        void payCallback_cancelledOrder_shouldThrowBusinessException() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.CANCELLED);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.payCallback(ORDER_NO, "ALIPAY"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持支付");
        }

        @Test
        @DisplayName("openBox：待支付订单不可开盒")
        void openBox_pendingOrder_shouldThrowBusinessException() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PENDING);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.openBox(USER_ID, ORDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅已支付");
        }

        @Test
        @DisplayName("openBox：已开盒订单直接开盒 → 抛异常（入口校验拦截）")
        void openBox_alreadyOpened_shouldThrowBusinessException() {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.OPENED);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.openBox(USER_ID, ORDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅已支付");
        }

        @Test
        @DisplayName("payCallback：订单号不存在 → 抛 ResourceNotFoundException")
        void payCallback_nonexistentOrder_shouldThrowResourceNotFoundException() {
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> service.payCallback("BB99999999999", "ALIPAY"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("openBox：分布式锁获取失败 → 抛 BusinessException")
        void openBox_lockAcquireFailed_shouldThrowBusinessException() throws InterruptedException {
            BlindBoxOrder order = buildOrder(ORDER_ID, ORDER_NO, OrderStatus.PAID);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

            assertThatThrownBy(() -> service.openBox(USER_ID, ORDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("系统繁忙");
        }
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    private BlindBoxOrder buildOrder(Long id, String orderNo, OrderStatus status) {
        return BlindBoxOrder.builder()
                .id(id)
                .userId(USER_ID)
                .templateId(TEMPLATE_ID)
                .orderNo(orderNo)
                .status(status)
                .payAmount(PAY_AMOUNT)
                .expireAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    private BlindBoxTemplate buildTemplate(Long id, BlindBoxType type, int stock) {
        return BlindBoxTemplate.builder()
                .id(id)
                .name("测试盲盒")
                .type(type)
                .price(PAY_AMOUNT)
                .stock(stock)
                .status(com.ctrip.blindbox.entity.enums.TemplateStatus.ACTIVE)
                .build();
    }

    private void mockLockAcquired() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
    }
}
