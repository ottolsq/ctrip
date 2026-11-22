package com.ctrip.blindbox.messaging;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.dto.FlashSaleMessage;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.BlindBoxPreferenceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("秒杀订单消费者测试")
class FlashSaleOrderConsumerTest {

    @Mock
    private BlindBoxTemplateMapper templateMapper;
    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxPreferenceService preferenceService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private FlashSaleOrderConsumer consumer;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
    }

    @BeforeEach
    void setUp() {
        consumer = new FlashSaleOrderConsumer(
                templateMapper, orderMapper, preferenceService, redisTemplate);
    }

    private FlashSaleMessage message() {
        return FlashSaleMessage.of(1L, 9L, "BB202611220001", "杭州", "STANDARD", "美食");
    }

    private void mockRedis(boolean acquired) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(acquired);
    }

    @Test
    @DisplayName("成功：扣减 MySQL 库存、创建 PAID 订单并保存偏好")
    void onCreate_success_shouldCreateOrderAndPreference() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        mockRedis(true);
        when(templateMapper.decrementStock(9L)).thenReturn(1);
        when(templateMapper.selectById(9L)).thenReturn(BlindBoxTemplate.builder()
                .id(9L).price(new BigDecimal("88.00")).build());

        consumer.onCreateOrder(message());

        verify(orderMapper).insert(any(BlindBoxOrder.class));
        verify(preferenceService).savePreference(
                any(), eq("杭州"), eq("STANDARD"), eq("美食"));
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("扣库存后失败：补偿一次并转死信")
    void onCreate_failureAfterDeduct_shouldCompensateOnceAndReject() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        mockRedis(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(templateMapper.decrementStock(9L)).thenReturn(1);
        when(templateMapper.selectById(9L)).thenReturn(BlindBoxTemplate.builder()
                .id(9L).price(new BigDecimal("88.00")).build());
        when(preferenceService.savePreference(any(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> consumer.onCreateOrder(message()))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(valueOperations).increment("seckill:stock:9");
        verify(setOperations).remove("seckill:users:9", "1");
    }

    @Test
    @DisplayName("MySQL 库存不足：不恢复 Redis，释放用户并转死信")
    void onCreate_rowsZero_shouldNotInflateRedisStock() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        mockRedis(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(templateMapper.decrementStock(9L)).thenReturn(0);

        assertThatThrownBy(() -> consumer.onCreateOrder(message()))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(valueOperations, never()).increment(anyString());
        verify(setOperations).remove("seckill:users:9", "1");
    }

    @Test
    @DisplayName("订单已存在：直接跳过，不重复扣库存")
    void onCreate_existingOrder_shouldSkip() {
        when(orderMapper.selectOne(any())).thenReturn(BlindBoxOrder.builder()
                .orderNo("BB202611220001").build());

        consumer.onCreateOrder(message());

        verify(templateMapper, never()).decrementStock(any());
    }
}
