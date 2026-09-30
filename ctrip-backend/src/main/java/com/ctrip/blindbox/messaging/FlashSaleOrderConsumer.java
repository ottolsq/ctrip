package com.ctrip.blindbox.messaging;

import com.ctrip.blindbox.dto.FlashSaleMessage;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *   <li><b>MQ 重试</b>——异常抛出后由 RabbitMQ 重试（3 次 + 死信队列）</li>
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

    private final BlindBoxTemplateMapper templateMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public FlashSaleOrderConsumer(BlindBoxTemplateMapper templateMapper,
                                   BlindBoxOrderMapper orderMapper,
                                   StringRedisTemplate stringRedisTemplate) {
        this.templateMapper = templateMapper;
        this.orderMapper = orderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 监听秒杀订单队列，异步创建订单。
     *
     * <p>注意：队列名 {@code flash.sale.order.queue} 在
     * {@code RabbitMQConfig} 中声明并绑定到 {@code flash.sale.exchange}。
     */
    @RabbitListener(queues = "flash.sale.order.queue")
    @Transactional
    public void onCreateOrder(FlashSaleMessage message) {
        log.info("收到秒杀订单消息: orderNo={}, userId={}, templateId={}",
                message.orderNo(), message.userId(), message.templateId());

        // 1. 幂等检查——同一用户同一模板只处理一次
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + message.userId() + ":" + message.templateId();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("消息已消费，跳过: orderNo={}", message.orderNo());
            return;
        }

        try {
            // 2. MySQL 乐观锁扣库存（最终一致性保证）
            int rows = templateMapper.decrementStock(message.templateId());
            if (rows == 0) {
                // 不应发生——Redis 已扣但 MySQL 库存不足
                rollbackRedisStock(message.templateId());
                log.error("秒杀库存异常(Redis已扣但MySQL不足): templateId={}, orderNo={}",
                        message.templateId(), message.orderNo());
                return;
            }

            // 3. 查询模板信息
            BlindBoxTemplate template = templateMapper.selectById(message.templateId());
            if (template == null) {
                rollbackRedisStock(message.templateId());
                log.error("秒杀模板不存在: templateId={}", message.templateId());
                return;
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

            log.info("秒杀订单创建成功: orderNo={}, userId={}, templateId={}, amount={}",
                    message.orderNo(), message.userId(), message.templateId(), template.getPrice());

        } catch (Exception e) {
            // 任何步骤失败 → 释放幂等锁(允许MQ重试) + 回滚Redis库存
            stringRedisTemplate.delete(idempotentKey);
            rollbackRedisStock(message.templateId());
            log.error("秒杀订单创建失败，已回滚: orderNo={}, templateId={}, error={}",
                    message.orderNo(), message.templateId(), e.getMessage(), e);
            throw e; // 触发 RabbitMQ 重试机制
        }
    }

    /**
     * 恢复 Redis 库存——MySQL 扣减失败时的回滚操作。
     *
     * <p>场景：
     * <ul>
     *   <li>MySQL 乐观锁返回 0（库存已空但 Redis 未及时同步）</li>
     *   <li>模板不存在（数据异常）</li>
     *   <li>订单插入失败（数据库异常）</li>
     * </ul>
     */
    private void rollbackRedisStock(Long templateId) {
        String stockKey = STOCK_KEY_PREFIX + templateId;
        stringRedisTemplate.opsForValue().increment(stockKey);
        log.warn("已回滚 Redis 库存: templateId={}", templateId);
    }
}
