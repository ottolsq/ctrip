package com.ctrip.blindbox.service;

import com.ctrip.blindbox.dto.FlashSaleMessage;
import com.ctrip.blindbox.dto.FlashSaleRequest;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("秒杀入口服务测试")
class FlashSaleServiceExecuteTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxTemplateMapper templateMapper;

    private FlashSaleService service;

    @BeforeEach
    void setUp() {
        service = new FlashSaleService(redisTemplate, rabbitTemplate, orderMapper, templateMapper);
    }

    private BlindBoxTemplate template(BlindBoxType type, TemplateStatus status,
                                      int startHours, int endHours) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String rule = "{\"activityStartTime\":\""
                + LocalDateTime.now().plusHours(startHours).format(fmt)
                + "\",\"activityEndTime\":\""
                + LocalDateTime.now().plusHours(endHours).format(fmt) + "\"}";
        return BlindBoxTemplate.builder()
                .id(9L).name("秒杀盲盒").type(type)
                .price(new BigDecimal("88.00")).stock(5)
                .status(status).ruleConfig(rule)
                .build();
    }

    private FlashSaleRequest request() {
        return new FlashSaleRequest(9L, "杭州", "STANDARD", "美食");
    }

    private void mockSuccess(BlindBoxTemplate template) {
        when(templateMapper.selectById(9L)).thenReturn(template);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("seckill:stock:9")).thenReturn(null);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(orderMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    @DisplayName("成功：懒预热库存、Lua 通过、消息携带偏好")
    void execute_shouldPrewarmAndSendMessageWithPreference() {
        mockSuccess(template(BlindBoxType.LIMITED, TemplateStatus.ACTIVE, -1, 2));

        FlashSaleService.FlashSaleResult result = service.execute(1L, request());

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(result.orderNo()).startsWith("BB");
        verify(valueOperations).set(
                eq("seckill:stock:9"), eq("5"), any(Duration.class));

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(FlashSaleService.FLASH_SALE_EXCHANGE),
                eq(FlashSaleService.FLASH_SALE_ORDER_RK), messageCaptor.capture());
        FlashSaleMessage message = (FlashSaleMessage) messageCaptor.getValue();
        assertThat(message.departureCity()).isEqualTo("杭州");
        assertThat(message.budgetLevel()).isEqualTo("STANDARD");
        assertThat(message.theme()).isEqualTo("美食");
    }

    @Test
    @DisplayName("非限定盲盒：拒绝")
    void execute_daily_shouldThrow() {
        when(templateMapper.selectById(9L)).thenReturn(
                template(BlindBoxType.DAILY, TemplateStatus.ACTIVE, -1, 2));

        assertThatThrownBy(() -> service.execute(1L, request()))
                .isInstanceOf(BusinessException.class);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("活动已结束：拒绝")
    void execute_expiredWindow_shouldThrow() {
        when(templateMapper.selectById(9L)).thenReturn(
                template(BlindBoxType.LIMITED, TemplateStatus.ACTIVE, -3, -2));

        assertThatThrownBy(() -> service.execute(1L, request()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("预算等级非法：拒绝")
    void execute_invalidBudget_shouldThrow() {
        when(templateMapper.selectById(9L)).thenReturn(
                template(BlindBoxType.LIMITED, TemplateStatus.ACTIVE, -1, 2));

        assertThatThrownBy(() -> service.execute(
                1L, new FlashSaleRequest(9L, "杭州", "BAD", null)))
                .isInstanceOf(BusinessException.class);
    }
}
