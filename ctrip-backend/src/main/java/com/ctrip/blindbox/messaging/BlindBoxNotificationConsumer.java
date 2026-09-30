package com.ctrip.blindbox.messaging;

import com.ctrip.messaging.event.BlindBoxOpenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 盲盒通知消费者——开盒方案生成完成后通知用户。
 *
 * <p>MVP 阶段以日志输出代替真实推送，后续接入站内信/短信/邮件
 * 只需修改本消费者，不影响盲盒开盒主流程。
 */
@Component
public class BlindBoxNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(BlindBoxNotificationConsumer.class);
    private static final String IDEMPOTENT_KEY = "mq:idempotent:bb-notify:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public BlindBoxNotificationConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(queues = "blindbox.notification.queue")
    public void onEvent(BlindBoxOpenEvent event) {
        String key = IDEMPOTENT_KEY + event.eventId();
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(ok)) return;

        String dest = event.metadata() != null
                ? String.valueOf(event.metadata().getOrDefault("destination", "未知")) : "未知";
        switch (event.eventType()) {
            case "BLINDBOX_RESULT_GENERATED" ->
                    log.info("[盲盒通知] 方案已生成: userId={}, destination={}", event.userId(), dest);
            case "BLINDBOX_SHARED" ->
                    log.info("[盲盒通知] 结果已分享: userId={}", event.userId());
        }
    }
}
