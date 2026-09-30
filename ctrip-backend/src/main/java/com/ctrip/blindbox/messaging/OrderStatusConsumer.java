package com.ctrip.blindbox.messaging;

import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.config.RabbitMQConfig;
import com.ctrip.messaging.event.OrderStatusEvent;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单状态事件消费者。
 *
 * <p>监听 order.status.queue，处理订单状态变更引发的下游业务逻辑：
 * <ul>
 *   <li>ORDER_PAID：记录支付事件日志，一致性校验</li>
 *   <li>ORDER_CANCELLED：记录取消事件</li>
 *   <li>ORDER_REFUNDED：恢复限定盲盒模板库存</li>
 * </ul>
 *
 * <p>通过 Redis SETNX 实现 eventId 幂等检查，防止重复消费。
 */
@Component
public class OrderStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConsumer.class);
    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:order:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final BlindBoxTemplateMapper templateMapper;
    private final RedissonClient redissonClient;

    public OrderStatusConsumer(BlindBoxTemplateMapper templateMapper, RedissonClient redissonClient) {
        this.templateMapper = templateMapper;
        this.redissonClient = redissonClient;
    }

    /**
     * 监听订单状态事件队列。
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_QUEUE)
    public void onOrderStatusEvent(OrderStatusEvent event) {
        log.info("收到订单状态事件: eventId={}, eventType={}, orderNo={}",
                event.eventId(), event.eventType(), event.orderNo());

        // 幂等检查
        if (!acquireIdempotentLock(event.eventId())) {
            log.info("事件已消费，跳过幂等处理: eventId={}", event.eventId());
            return;
        }

        try {
            switch (event.eventType()) {
                case "ORDER_PAID" -> handleOrderPaid(event);
                case "ORDER_CANCELLED" -> handleOrderCancelled(event);
                case "ORDER_REFUNDED" -> handleOrderRefunded(event);
                default -> log.info("未处理的订单事件类型: {}", event.eventType());
            }
        } catch (Exception e) {
            // 处理失败，释放幂等锁以允许重试
            releaseIdempotentLock(event.eventId());
            log.error("处理订单状态事件失败: eventId={}, error={}", event.eventId(), e.getMessage(), e);
            throw e; // 让 RabbitMQ 重试机制接管
        }
    }

    /**
     * 处理订单支付成功事件。
     */
    private void handleOrderPaid(OrderStatusEvent event) {
        log.info("订单已支付: orderNo={}, userId={}, amount={}, payMethod={}",
                event.orderNo(), event.userId(), event.payAmount(), event.payMethod());

        if (event.metadata() != null && event.metadata().containsKey("transactionId")) {
            log.info("支付交易号: {}", event.metadata().get("transactionId"));
        }
    }

    /**
     * 处理订单取消事件。
     */
    private void handleOrderCancelled(OrderStatusEvent event) {
        log.info("订单已取消: orderNo={}, userId={}", event.orderNo(), event.userId());
    }

    /**
     * 处理订单退款事件。
     */
    private void handleOrderRefunded(OrderStatusEvent event) {
        log.info("订单已退款: orderNo={}, userId={}", event.orderNo(), event.userId());
        // 限定盲盒恢复库存
        templateMapper.incrementStock(event.templateId());
        log.info("退款恢复库存: templateId={}", event.templateId());
    }

    /**
     * 通过 Redis SETNX 实现幂等锁。
     *
     * @return true 表示首次消费，false 表示已消费过
     */
    private boolean acquireIdempotentLock(String eventId) {
        String key = IDEMPOTENT_KEY_PREFIX + eventId;
        Boolean acquired = redissonClient.getBucket(key)
                .setIfAbsent("processed", IDEMPOTENT_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放幂等锁（处理失败时调用，允许重试）。
     */
    private void releaseIdempotentLock(String eventId) {
        String key = IDEMPOTENT_KEY_PREFIX + eventId;
        redissonClient.getBucket(key).delete();
    }
}
