package com.ctrip.messaging.consumer;

import com.ctrip.messaging.event.ContentAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 内容审核消费者——处理攻略提交/审核通过/审核拒绝事件。
 *
 * <p>MVP 阶段以日志输出，后续可接入自动审核机审或对接人工审核后台。
 */
@Component
public class ContentAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContentAuditConsumer.class);
    private static final String IDEMPOTENT_KEY = "mq:idempotent:audit:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public ContentAuditConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(queues = "content.audit.queue")
    public void onAudit(ContentAuditEvent event) {
        String key = IDEMPOTENT_KEY + event.eventId();
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(ok)) return;

        switch (event.eventType()) {
            case "GUIDE_SUBMITTED" ->
                    log.info("[内容审核] 攻略提交: guideId={}, authorId={}",
                            event.guideId(), event.authorId());
            case "GUIDE_APPROVED" ->
                    log.info("[内容审核] 审核通过: guideId={}, operatorId={}",
                            event.guideId(), event.operatorId());
            case "GUIDE_REJECTED" ->
                    log.info("[内容审核] 审核拒绝: guideId={}, reason={}",
                            event.guideId(), event.reason());
        }
    }
}
