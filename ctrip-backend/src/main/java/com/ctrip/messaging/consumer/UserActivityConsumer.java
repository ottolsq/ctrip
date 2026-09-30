package com.ctrip.messaging.consumer;

import com.ctrip.config.RabbitMQConfig;
import com.ctrip.messaging.event.UserActivityEvent;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户行为事件消费者。
 *
 * <p>监听 user.activity.queue，记录用户行为日志。
 * 当前阶段仅做日志输出，后续可对接数据分析系统或推荐引擎。
 *
 * <p>通过 Redis SETNX 实现幂等检查，防止重复消费。
 */
@Component
public class UserActivityConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserActivityConsumer.class);
    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:user:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final RedissonClient redissonClient;

    public UserActivityConsumer(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 监听用户行为事件队列。
     */
    @RabbitListener(queues = RabbitMQConfig.USER_ACTIVITY_QUEUE)
    public void onUserActivityEvent(UserActivityEvent event) {
        // 幂等检查
        if (!acquireIdempotentLock(event.eventId())) {
            log.info("事件已消费，跳过幂等处理: eventId={}", event.eventId());
            return;
        }

        log.info("收到用户行为事件: eventId={}, eventType={}, userId={}, targetType={}, targetId={}",
                event.eventId(), event.eventType(), event.userId(),
                event.targetType(), event.targetId());

        if (event.metadata() != null && !event.metadata().isEmpty()) {
            log.info("  元数据: {}", event.metadata());
        }
    }

    /**
     * 通过 Redis SETNX 实现幂等锁。
     */
    private boolean acquireIdempotentLock(String eventId) {
        String key = IDEMPOTENT_KEY_PREFIX + eventId;
        Boolean acquired = redissonClient.getBucket(key)
                .setIfAbsent("processed", IDEMPOTENT_TTL);
        return Boolean.TRUE.equals(acquired);
    }
}
