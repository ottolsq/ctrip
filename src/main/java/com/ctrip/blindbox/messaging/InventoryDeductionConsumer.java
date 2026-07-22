package com.ctrip.blindbox.messaging;

import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.messaging.event.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 库存扣减消费者——支付成功时异步扣减 MySQL 库存。
 *
 * <p>将库存扣减从支付回调中解耦，支付成功后通过 MQ 异步执行，
 * 降低支付接口响应时间。仅处理限定盲盒（LIMITED）的库存扣减。
 */
@Component
public class InventoryDeductionConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryDeductionConsumer.class);

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:inventory:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final BlindBoxTemplateMapper templateMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public InventoryDeductionConsumer(BlindBoxTemplateMapper templateMapper,
                                       StringRedisTemplate stringRedisTemplate) {
        this.templateMapper = templateMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 监听库存扣减队列，仅处理 ORDER_PAID 事件。
     */
    @RabbitListener(queues = "inventory.deduction.queue")
    public void onOrderPaid(OrderStatusEvent event) {
        if (!"ORDER_PAID".equals(event.eventType())) {
            return;
        }

        // 幂等检查
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + event.eventId();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        // 仅扣减限定盲盒库存
        BlindBoxTemplate template = templateMapper.selectById(event.templateId());
        if (template == null || template.getType() != BlindBoxType.LIMITED) {
            return;
        }

        try {
            int rows = templateMapper.decrementStock(event.templateId());
            if (rows == 0) {
                log.error("库存扣减失败(乐观锁): templateId={}, orderNo={}",
                        event.templateId(), event.orderNo());
            } else {
                log.info("异步库存扣减成功: templateId={}, orderNo={}",
                        event.templateId(), event.orderNo());
            }
        } catch (Exception e) {
            stringRedisTemplate.delete(idempotentKey);
            log.error("库存扣减异常: templateId={}, error={}",
                    event.templateId(), e.getMessage(), e);
            throw e;
        }
    }
}
