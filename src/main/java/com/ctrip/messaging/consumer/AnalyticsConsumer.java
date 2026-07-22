package com.ctrip.messaging.consumer;

import com.ctrip.messaging.event.UserActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户行为分析消费者——记录用户行为事件用于运营分析。
 *
 * <p>MVP 阶段以日志输出代替数据持久化，后续可接入独立统计表
 * 或数据仓库（ClickHouse 等）用于运营仪表盘和转化漏斗分析。
 */
@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String IDEMPOTENT_KEY = "mq:idempotent:analytics:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public AnalyticsConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(queues = "analytics.queue")
    public void onActivity(UserActivityEvent event) {
        String key = IDEMPOTENT_KEY + event.eventId();
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(ok)) return;

        switch (event.eventType()) {
            case "USER_REGISTERED" ->
                    log.info("[行为分析] 新用户注册: userId={}", event.userId());
            case "GUIDE_VIEWED" ->
                    log.info("[行为分析] 攻略浏览: userId={}, guideId={}", event.userId(), event.targetId());
            case "GUIDE_LIKED" ->
                    log.info("[行为分析] 攻略点赞: userId={}, guideId={}", event.userId(), event.targetId());
            case "ITINERARY_CREATED" ->
                    log.info("[行为分析] 行程创建: userId={}", event.userId());
            case "BLINDBOX_PURCHASED" ->
                    log.info("[行为分析] 盲盒购买: userId={}", event.userId());
            case "ITINERARY_SHARED" ->
                    log.info("[行为分析] 行程分享: userId={}", event.userId());
        }
    }
}
