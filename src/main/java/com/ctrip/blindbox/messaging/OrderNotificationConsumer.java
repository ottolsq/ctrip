package com.ctrip.blindbox.messaging;

import com.ctrip.messaging.event.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单通知消费者——支付成功 / 开盒完成时发送用户通知。
 *
 * <p>MVP 阶段以日志输出代替真实推送（短信/邮件/站内信），
 * 后续接入真实推送渠道时只需修改本消费者，不影响其他模块。
 */
@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:notify:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public OrderNotificationConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 监听订单通知队列，处理支付成功和开盒完成事件。
     */
    @RabbitListener(queues = "order.notification.queue")
    public void onOrderEvent(OrderStatusEvent event) {
        // 幂等检查
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + event.eventId();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        switch (event.eventType()) {
            case "ORDER_PAID" -> log.info("[通知] 支付成功: userId={}, orderNo={}, amount={}",
                    event.userId(), event.orderNo(), event.payAmount());
            case "ORDER_OPENED" -> log.info("[通知] 盲盒已就绪: userId={}, orderNo={}",
                    event.userId(), event.orderNo());
            case "ORDER_CANCELLED" -> log.info("[通知] 订单已取消: userId={}, orderNo={}",
                    event.userId(), event.orderNo());
            case "ORDER_REFUNDED" -> log.info("[通知] 已退款: userId={}, orderNo={}",
                    event.userId(), event.orderNo());
        }
    }
}
