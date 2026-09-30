package com.ctrip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类。
 *
 * <p>声明 Exchange、Queue、Binding 及消息序列化配置。
 * 按照 redis_rabbitmq_design.md 文档设计：
 * <ul>
 *   <li>order.status.events — 订单状态事件流</li>
 *   <li>blindbox.open.events — 盲盒开盒事件流</li>
 *   <li>user.activity.events — 用户行为事件流</li>
 *   <li>content.audit.events — 内容审核事件流</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ========== Exchange 名称常量 ==========

    public static final String ORDER_STATUS_EXCHANGE = "order.status.events";
    public static final String BLINDBOX_OPEN_EXCHANGE = "blindbox.open.events";
    public static final String USER_ACTIVITY_EXCHANGE = "user.activity.events";
    public static final String CONTENT_AUDIT_EXCHANGE = "content.audit.events";

    // ========== DLX ==========

    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String ORDER_STATUS_DLQ = "order.status.events.dlq";

    // ========== Queue 名称常量 ==========

    // 订单状态
    public static final String ORDER_STATUS_QUEUE = "order.status.queue";
    public static final String ORDER_NOTIFICATION_QUEUE = "order.notification.queue";
    public static final String INVENTORY_DEDUCTION_QUEUE = "inventory.deduction.queue";

    // 盲盒开盒
    public static final String BLINDBOX_RESULT_GEN_QUEUE = "blindbox.result.gen.queue";
    public static final String BLINDBOX_NOTIFICATION_QUEUE = "blindbox.notification.queue";

    // 用户活动
    public static final String USER_ACTIVITY_QUEUE = "user.activity.queue";
    public static final String ANALYTICS_QUEUE = "analytics.queue";

    // 内容审核
    public static final String CONTENT_AUDIT_QUEUE = "content.audit.queue";

    // ========== Routing Key ==========

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_PAID = "order.paid";
    public static final String RK_ORDER_OPENED = "order.opened";
    public static final String RK_ORDER_REFUNDED = "order.refunded";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";

    public static final String RK_BLINDBOX_REQUESTED = "blindbox.requested";
    public static final String RK_BLINDBOX_RESULT = "blindbox.result";
    public static final String RK_BLINDBOX_SHARED = "blindbox.shared";

    public static final String RK_USER_ACTIVITY = "user.activity.#";

    public static final String RK_CONTENT_AUDIT = "content.audit.#";

    // ========== 消息转换器 ==========

    /**
     * JSON 消息转换器 — 使用 Jackson 将对象序列化为 JSON 消息体。
     *
     * <p>配置 JavaTimeModule 以支持 OffsetDateTime 等 Java 8 时间类型，
     * 并禁用 WRITE_DATES_AS_TIMESTAMPS 以输出 ISO 8601 格式。
     * Spring Boot 3.5 + Jackson 2.17+ 原生支持 Java record 反序列化。
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 自定义 Listener Container Factory，启用 JSON 转换器。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        return factory;
    }

    // ========== DLX Exchange & Queue ==========

    @Bean
    public FanoutExchange dlxExchange() {
        return new FanoutExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStatusDlq() {
        return QueueBuilder.durable(ORDER_STATUS_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue orderStatusDlq, FanoutExchange dlxExchange) {
        return BindingBuilder.bind(orderStatusDlq).to(dlxExchange);
    }

    // ========== Order Status Exchange & Queues ==========

    @Bean
    public TopicExchange orderStatusExchange() {
        return new TopicExchange(ORDER_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(ORDER_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_STATUS_DLQ)
                .build();
    }

    @Bean
    public Queue orderNotificationQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue inventoryDeductionQueue() {
        return QueueBuilder.durable(INVENTORY_DEDUCTION_QUEUE).build();
    }

    @Bean
    public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange orderStatusExchange) {
        return BindingBuilder.bind(orderStatusQueue).to(orderStatusExchange).with("#");
    }

    @Bean
    public Binding orderNotificationBinding(Queue orderNotificationQueue, TopicExchange orderStatusExchange) {
        return BindingBuilder.bind(orderNotificationQueue).to(orderStatusExchange).with("#");
    }

    @Bean
    public Binding inventoryDeductionBinding(Queue inventoryDeductionQueue, TopicExchange orderStatusExchange) {
        return BindingBuilder.bind(inventoryDeductionQueue).to(orderStatusExchange).with("order.paid");
    }

    // ========== BlindBox Open Exchange & Queues ==========

    @Bean
    public TopicExchange blindboxOpenExchange() {
        return new TopicExchange(BLINDBOX_OPEN_EXCHANGE, true, false);
    }

    @Bean
    public Queue blindboxResultGenQueue() {
        return QueueBuilder.durable(BLINDBOX_RESULT_GEN_QUEUE).build();
    }

    @Bean
    public Queue blindboxNotificationQueue() {
        return QueueBuilder.durable(BLINDBOX_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding blindboxResultGenBinding(Queue blindboxResultGenQueue, TopicExchange blindboxOpenExchange) {
        return BindingBuilder.bind(blindboxResultGenQueue).to(blindboxOpenExchange).with("#");
    }

    @Bean
    public Binding blindboxNotificationBinding(Queue blindboxNotificationQueue, TopicExchange blindboxOpenExchange) {
        return BindingBuilder.bind(blindboxNotificationQueue).to(blindboxOpenExchange).with("#");
    }

    // ========== User Activity Exchange & Queues ==========

    @Bean
    public TopicExchange userActivityExchange() {
        return new TopicExchange(USER_ACTIVITY_EXCHANGE, true, false);
    }

    @Bean
    public Queue userActivityQueue() {
        return QueueBuilder.durable(USER_ACTIVITY_QUEUE).build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Binding userActivityBinding(Queue userActivityQueue, TopicExchange userActivityExchange) {
        return BindingBuilder.bind(userActivityQueue).to(userActivityExchange).with("#");
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange userActivityExchange) {
        return BindingBuilder.bind(analyticsQueue).to(userActivityExchange).with("#");
    }

    // ========== Content Audit Exchange & Queue ==========

    @Bean
    public TopicExchange contentAuditExchange() {
        return new TopicExchange(CONTENT_AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public Queue contentAuditQueue() {
        return QueueBuilder.durable(CONTENT_AUDIT_QUEUE).build();
    }

    // ========== Flash Sale Exchange & Queue ==========

    /** 秒杀订单 Exchange */
    public static final String FLASH_SALE_EXCHANGE = "flash.sale.exchange";

    /** 秒杀订单队列 */
    public static final String FLASH_SALE_ORDER_QUEUE = "flash.sale.order.queue";

    @Bean
    public TopicExchange flashSaleExchange() {
        return new TopicExchange(FLASH_SALE_EXCHANGE, true, false);
    }

    @Bean
    public Queue flashSaleOrderQueue() {
        return QueueBuilder.durable(FLASH_SALE_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FLASH_SALE_ORDER_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Binding flashSaleOrderBinding(Queue flashSaleOrderQueue, TopicExchange flashSaleExchange) {
        return BindingBuilder.bind(flashSaleOrderQueue).to(flashSaleExchange).with("#");
    }

    @Bean
    public Binding contentAuditBinding(Queue contentAuditQueue, TopicExchange contentAuditExchange) {
        return BindingBuilder.bind(contentAuditQueue).to(contentAuditExchange).with("#");
    }
}
