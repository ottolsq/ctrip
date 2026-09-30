package com.ctrip.messaging.publisher;

import com.ctrip.config.RabbitMQConfig;
import com.ctrip.messaging.event.BlindBoxOpenEvent;
import com.ctrip.messaging.event.ContentAuditEvent;
import com.ctrip.messaging.event.OrderStatusEvent;
import com.ctrip.messaging.event.UserActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 通用事件发布器。
 *
 * <p>封装 RabbitTemplate，向各 Exchange 发布业务事件。
 * MQ 不可用时降级为日志输出，不阻塞主流程。
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布订单状态事件。
     *
     * @param eventType  事件类型（ORDER_CREATED / ORDER_PAID / ORDER_OPENED / ORDER_REFUNDED / ORDER_CANCELLED）
     * @param orderId    订单 ID
     * @param orderNo    订单号
     * @param userId     用户 ID
     * @param templateId 盲盒模板 ID
     * @param payAmount  支付金额
     * @param payMethod  支付方式
     * @param metadata   附加元数据
     */
    public void publishOrderStatusEvent(String eventType, Long orderId, String orderNo,
                                        Long userId, Long templateId, BigDecimal payAmount,
                                        String payMethod, Map<String, Object> metadata) {
        OrderStatusEvent event = OrderStatusEvent.of(
                eventType, orderId, orderNo, userId, templateId, payAmount, payMethod, metadata);
        publish(RabbitMQConfig.ORDER_STATUS_EXCHANGE, resolveRoutingKey(eventType), event);
    }

    /**
     * 发布盲盒开盒事件。
     *
     * @param eventType 事件类型（BLINDBOX_OPEN_REQUESTED / BLINDBOX_RESULT_GENERATED / BLINDBOX_SHARED）
     * @param orderId   订单 ID
     * @param userId    用户 ID
     * @param resultId  结果 ID
     * @param payload   附加数据
     */
    public void publishBlindBoxOpenEvent(String eventType, Long orderId, Long userId,
                                         Long resultId, Map<String, Object> payload) {
        BlindBoxOpenEvent event = BlindBoxOpenEvent.of(eventType, orderId, userId, resultId, payload);
        publish(RabbitMQConfig.BLINDBOX_OPEN_EXCHANGE, resolveRoutingKey(eventType), event);
    }

    /**
     * 发布用户行为事件。
     *
     * @param eventType  事件类型（USER_REGISTERED / GUIDE_VIEWED / GUIDE_LIKED / 等）
     * @param userId     用户 ID
     * @param targetId   目标实体 ID
     * @param targetType 目标实体类型（GUIDE / ITINERARY / BLINDBOX）
     * @param metadata   附加元数据
     */
    public void publishUserActivityEvent(String eventType, Long userId, Long targetId,
                                         String targetType, Map<String, Object> metadata) {
        UserActivityEvent event = UserActivityEvent.of(eventType, userId, targetId, targetType, metadata);
        publish(RabbitMQConfig.USER_ACTIVITY_EXCHANGE, "user.activity." + eventType.toLowerCase(), event);
    }

    /**
     * 发布内容审核事件。
     *
     * @param eventType  事件类型（GUIDE_SUBMITTED / GUIDE_APPROVED / GUIDE_REJECTED）
     * @param guideId    攻略 ID
     * @param authorId   作者 ID
     * @param operatorId 审核操作人 ID
     * @param reason     审核原因
     */
    public void publishContentAuditEvent(String eventType, Long guideId, Long authorId,
                                         Long operatorId, String reason) {
        ContentAuditEvent event = ContentAuditEvent.of(eventType, guideId, authorId, operatorId, reason);
        publish(RabbitMQConfig.CONTENT_AUDIT_EXCHANGE, "content.audit." + eventType.toLowerCase(), event);
    }

    /**
     * 向指定 Exchange 发布事件。
     *
     * <p>连接失败时抛出 RuntimeException，使调用方事务回滚（防止订单卡在 PROCESSING 状态）。
     * 其他异常（如序列化失败）同样抛出，因为消息未发出意味着后续异步流程无法完成。
     */
    private void publish(String exchange, String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("发布事件: exchange={}, routingKey={}", exchange, routingKey);
        } catch (AmqpConnectException e) {
            log.error("RabbitMQ 连接失败: exchange={}, routingKey={}", exchange, routingKey, e);
            throw new RuntimeException("消息队列不可用，请稍后重试", e);
        } catch (Exception e) {
            log.error("发布事件失败: exchange={}, routingKey={}", exchange, routingKey, e);
            throw new RuntimeException("事件发布失败", e);
        }
    }

    /**
     * 根据 eventType 解析 routing key。
     * 将 ORDER_CREATED → order.created，BLINDBOX_OPEN_REQUESTED → blindbox.requested 等。
     */
    private String resolveRoutingKey(String eventType) {
        return eventType.toLowerCase().replace("_", ".");
    }
}
