package com.ctrip.blindbox.messaging;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxPreference;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.service.BlindBoxPreferenceService;
import com.ctrip.blindbox.service.BlindBoxSchemeService;
import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.messaging.event.BlindBoxOpenEvent;
import com.ctrip.messaging.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 盲盒结果生成消费者——异步调用 AI 生成旅行方案。
 *
 * <p>这是第3轮改造的核心：将 AI 调用从用户请求线程中移出，
 * 改为通过 RabbitMQ 异步消费。用户请求立即返回 PROCESSING 状态，
 * 本消费者在后台完成 AI 生成后更新状态为 OPENED。
 *
 * <h3>改造前后对比</h3>
 * <pre>
 * 改造前：POST /open → [加锁→调AI(5-20s)→保存→解锁] → 返回
 * 改造后：POST /open → [加锁→PROCESSING→发MQ→解锁] → 立即返回(~50ms)
 *                ↓ (MQ异步)
 *          本消费者 → [调AI→保存→OPENED→发通知]
 * </pre>
 *
 * <h3>可靠性保障</h3>
 * <ol>
 *   <li>Redis SETNX 幂等——同一 eventId 只处理一次</li>
 *   <li>AI 调用失败自动回退到 mock 方案——不抛异常，保证最终完成</li>
 *   <li>异常时释放幂等锁让 MQ 重试接管</li>
 * </ol>
 */
@Component
public class BlindBoxResultGenConsumer {

    private static final Logger log = LoggerFactory.getLogger(BlindBoxResultGenConsumer.class);

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:open:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxResultMapper resultMapper;
    private final BlindBoxPreferenceService preferenceService;
    private final BlindBoxSchemeService schemeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    public BlindBoxResultGenConsumer(BlindBoxOrderMapper orderMapper,
                                      BlindBoxResultMapper resultMapper,
                                      BlindBoxPreferenceService preferenceService,
                                      BlindBoxSchemeService schemeService,
                                      StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      EventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.resultMapper = resultMapper;
        this.preferenceService = preferenceService;
        this.schemeService = schemeService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 监听盲盒开盒请求队列，异步生成旅行方案。
     *
     * <p>消费 {@code blindbox.result.gen.queue} 中的 BLINDBOX_OPEN_REQUESTED 事件。
     * AI 调用可能耗时 5-20s，此过程不阻塞任何用户请求。
     */
    @RabbitListener(queues = "blindbox.result.gen.queue")
    @Transactional
    public void onOpenRequested(BlindBoxOpenEvent event) {
        log.info("收到开盒请求: eventId={}, orderId={}", event.eventId(), event.orderId());

        // 1. 幂等检查
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + event.eventId();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "processing", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("事件已消费，跳过: eventId={}", event.eventId());
            return;
        }

        try {
            // 2. 确认订单状态仍为 PROCESSING
            BlindBoxOrder order = orderMapper.selectById(event.orderId());
            if (order == null || order.getStatus() != OrderStatus.PROCESSING) {
                log.info("订单状态非PROCESSING，跳过: orderId={}, status={}",
                        event.orderId(), order != null ? order.getStatus() : "null");
                return;
            }

            // 3. 查询偏好参数 → 调用 AI 生成方案（核心耗时操作）
            BlindBoxPreference preference = preferenceService.getByOrderId(order.getId());
            SchemeResponse scheme = schemeService.generateScheme(preference);

            // 4. 序列化方案为 JSON 存入结果表
            String resultText;
            try {
                resultText = objectMapper.writeValueAsString(scheme);
            } catch (Exception e) {
                log.error("方案序列化失败: orderId={}", order.getId(), e);
                throw new RuntimeException("序列化方案失败", e);
            }

            // 5. 保存盲盒结果
            BlindBoxResult result = BlindBoxResult.builder()
                    .orderId(order.getId())
                    .destination(scheme.destination())
                    .destinationId(scheme.destinationId())
                    .theme(scheme.theme())
                    .resultText(resultText)
                    .openedAt(LocalDateTime.now())
                    .build();
            resultMapper.insert(result);

            // 6. 更新订单状态 PROCESSING → OPENED
            orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                    .eq(BlindBoxOrder::getId, order.getId())
                    .set(BlindBoxOrder::getStatus, OrderStatus.OPENED));

            log.info("异步开盒完成: orderNo={}, destination={}, theme={}",
                    order.getOrderNo(), scheme.destination(), scheme.theme());

            // 7. 发布 ORDER_OPENED 和 BLINDBOX_RESULT_GENERATED 事件
            eventPublisher.publishOrderStatusEvent(
                    "ORDER_OPENED", order.getId(), order.getOrderNo(), order.getUserId(),
                    order.getTemplateId(), order.getPayAmount(), order.getPayMethod(), null);

            eventPublisher.publishBlindBoxOpenEvent(
                    "BLINDBOX_RESULT_GENERATED", order.getId(), order.getUserId(),
                    result.getId(), Map.of(
                            "destination", scheme.destination(),
                            "theme", scheme.theme()));

            // 更新幂等状态
            stringRedisTemplate.opsForValue().set(idempotentKey, "completed", IDEMPOTENT_TTL);

        } catch (Exception e) {
            // 释放幂等锁，让 MQ 重试
            stringRedisTemplate.delete(idempotentKey);
            log.error("异步开盒失败: eventId={}, orderId={}, error={}",
                    event.eventId(), event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
