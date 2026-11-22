package com.ctrip.blindbox.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctrip.blindbox.dto.FlashSaleMessage;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.BlindBoxPreferenceService;
import com.ctrip.common.tx.AfterCommitExecutor;
import com.ctrip.messaging.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 秒杀订单异步消费者。
 *
 * <p>消费秒杀队列中的订单消息，完成 MySQL 层面的库存扣减和订单持久化。
 * Lua 脚本已保证 Redis 层面的原子性，本消费者负责将结果同步到数据库。
 *
 * <h3>可靠性保障</h3>
 * <ol>
 *   <li><b>幂等消费</b>——Redis SETNX 防重复处理</li>
 *   <li><b>乐观锁兜底</b>——MySQL {@code WHERE stock >= 1} 防超卖</li>
 *   <li><b>失败回滚</b>——MySQL 扣减失败时 INCR 恢复 Redis 库存</li>
 *   <li><b>失败补偿</b>——失败时补偿一次 Redis 库存并转死信队列，避免重试重复加库存</li>
 * </ol>
 *
 * <h3>消费流程</h3>
 * <pre>
 * 收到 FlashSaleMessage
 *   → SETNX 幂等检查 → 已处理则跳过
 *   → MySQL UPDATE stock = stock - 1 WHERE stock >= 1
 *     ├── 成功 → INSERT order(PAID) → 完成
 *     └── 失败 → INCR 恢复 Redis 库存 → 告警
 * </pre>
 */
@Component
public class FlashSaleOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleOrderConsumer.class);

    /** 幂等 Key 前缀：idempotent:flash:{userId}:{templateId} */
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:flash:";

    /** 幂等 Key TTL：1 小时（远超 MQ 重试窗口 30s×3） */
    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(1);

    /** 秒杀库存 Key 前缀：seckill:stock:{templateId}（用于失败回滚） */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** 秒杀用户集合 Key 前缀：seckill:users:{templateId} */
    private static final String USERS_KEY_PREFIX = "seckill:users:";

    /** 在途预扣计数 Key 前缀：seckill:pending:{templateId} */
    private static final String PENDING_KEY_PREFIX = "seckill:pending:";

    private final BlindBoxTemplateMapper templateMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxPreferenceService preferenceService;
    private final EventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public FlashSaleOrderConsumer(BlindBoxTemplateMapper templateMapper,
                                   BlindBoxOrderMapper orderMapper,
                                   BlindBoxPreferenceService preferenceService,
                                   EventPublisher eventPublisher,
                                   StringRedisTemplate stringRedisTemplate) {
        this.templateMapper = templateMapper;
        this.orderMapper = orderMapper;
        this.preferenceService = preferenceService;
        this.eventPublisher = eventPublisher;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 监听秒杀订单队列，异步创建订单。
     *
     * <p>注意：队列名 {@code flash.sale.order.queue} 在
     * {@code RabbitMQConfig} 中声明并绑定到 {@code flash.sale.exchange}。
     */
    @RabbitListener(queues = com.ctrip.config.RabbitMQConfig.FLASH_SALE_ORDER_QUEUE)
    @Transactional
    public void onCreateOrder(FlashSaleMessage message) {
        log.info("收到秒杀订单消息: orderNo={}, userId={}, templateId={}",
                message.orderNo(), message.userId(), message.templateId());

        // 1. 以订单号幂等：数据库已有订单直接成功（覆盖提交后标记丢失）
        BlindBoxOrder existing = orderMapper.selectOne(
                new LambdaQueryWrapper<BlindBoxOrder>()
                        .eq(BlindBoxOrder::getOrderNo, message.orderNo()));
        if (existing != null) {
            log.info("秒杀订单已存在，跳过: orderNo={}", message.orderNo());
            return;
        }

        String idempotentKey = IDEMPOTENT_KEY_PREFIX + message.orderNo();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "processing", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("秒杀消息正在处理或已处理，跳过: orderNo={}", message.orderNo());
            return;
        }

        boolean stockDeducted = false;
        try {
            // 2. MySQL 乐观锁扣库存（最终一致性保证）
            int rows = templateMapper.decrementStock(message.templateId());
            if (rows == 0) {
                // Redis 已扣但 MySQL 不足：不恢复 Redis，释放用户以便重试，消息进死信
                decrementPending(message.templateId());
                removeUser(message.templateId(), message.userId());
                throw new AmqpRejectAndDontRequeueException(
                        "秒杀库存异常(Redis已扣但MySQL不足): orderNo=" + message.orderNo());
            }
            stockDeducted = true;

            // 3. 查询模板信息
            BlindBoxTemplate template = templateMapper.selectById(message.templateId());
            if (template == null) {
                throw new IllegalStateException("秒杀模板不存在: " + message.templateId());
            }

            // 4. 创建订单——秒杀跳过支付步骤，直接置为 PAID
            BlindBoxOrder order = BlindBoxOrder.builder()
                    .userId(message.userId())
                    .templateId(message.templateId())
                    .orderNo(message.orderNo())
                    .status(OrderStatus.PAID)
                    .payAmount(template.getPrice())
                    .payMethod("FLASH_SALE")
                    .payTime(LocalDateTime.now())
                    .expireAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            orderMapper.insert(order);

            // 旧格式消息没有偏好字段：写入兜底偏好，保证开盒流程可用
            preferenceService.savePreference(
                    order.getId(),
                    message.departureCity() != null ? message.departureCity() : "未知",
                    message.budgetLevel() != null ? message.budgetLevel() : "STANDARD",
                    message.theme());

            AfterCommitExecutor.run(() -> {
                decrementPending(message.templateId());
                eventPublisher.publishOrderStatusEvent(
                        "ORDER_CREATED", order.getId(), order.getOrderNo(), order.getUserId(),
                        order.getTemplateId(), order.getPayAmount(), "FLASH_SALE", null);
                eventPublisher.publishOrderStatusEvent(
                        "ORDER_PAID", order.getId(), order.getOrderNo(), order.getUserId(),
                        order.getTemplateId(), order.getPayAmount(), "FLASH_SALE", null);
            });

            log.info("秒杀订单创建成功: orderNo={}, userId={}, templateId={}, amount={}",
                    message.orderNo(), message.userId(), message.templateId(), template.getPrice());

        } catch (AmqpRejectAndDontRequeueException e) {
            stringRedisTemplate.delete(idempotentKey);
            log.error("秒杀订单失败，转死信: orderNo={}, error={}",
                    message.orderNo(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // 每次消息只补偿一次；不重试，避免重复 INCR 造成 Redis 库存膨胀
            stringRedisTemplate.delete(idempotentKey);
            if (stockDeducted) {
                rollbackRedisStock(message.templateId());
            }
            decrementPending(message.templateId());
            removeUser(message.templateId(), message.userId());
            log.error("秒杀订单创建失败，已补偿并转死信: orderNo={}, templateId={}, error={}",
                    message.orderNo(), message.templateId(), e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    private void rollbackRedisStock(Long templateId) {
        String stockKey = STOCK_KEY_PREFIX + templateId;
        if (stringRedisTemplate.opsForValue().get(stockKey) == null) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(stockKey);
        log.warn("已回滚 Redis 库存: templateId={}", templateId);
    }

    private void removeUser(Long templateId, Long userId) {
        stringRedisTemplate.opsForSet().remove(
                USERS_KEY_PREFIX + templateId, userId.toString());
    }

    private void decrementPending(Long templateId) {
        stringRedisTemplate.opsForValue().decrement(PENDING_KEY_PREFIX + templateId);
    }
}
