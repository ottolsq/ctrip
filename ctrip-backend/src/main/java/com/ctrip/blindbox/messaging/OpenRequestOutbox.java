package com.ctrip.blindbox.messaging;

import com.ctrip.config.RabbitMQConfig;
import com.ctrip.messaging.event.BlindBoxOpenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OpenRequestOutbox {

    public static final String OPEN_REQUESTED = "BLINDBOX_OPEN_REQUESTED";
    private static final Logger log = LoggerFactory.getLogger(OpenRequestOutbox.class);

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    public OpenRequestOutbox(JdbcTemplate jdbc, RabbitTemplate rabbit) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Long orderId, Long userId) {
        var now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO blind_box_open_outbox (event_id, order_id, user_id, created_at, next_attempt_at)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), orderId, userId, now, now);
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 15000)
    public void dispatch() {
        var events = jdbc.query("""
                SELECT event_id, order_id, user_id, created_at FROM blind_box_open_outbox
                WHERE published_at IS NULL AND next_attempt_at <= ?
                ORDER BY next_attempt_at, event_id LIMIT 20
                """, (row, index) -> new BlindBoxOpenEvent(row.getString("event_id"), OPEN_REQUESTED,
                row.getLong("order_id"), row.getLong("user_id"), null,
                row.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC), null),
                Timestamp.from(Instant.now()));

        for (var event : events) {
            try {
                publishAndConfirm(event);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                log.warn("开盒请求待重投: eventId={}, orderId={}, error={}",
                        event.eventId(), event.orderId(), error.toString());
                jdbc.update("""
                        UPDATE blind_box_open_outbox SET next_attempt_at = ?
                        WHERE event_id = ? AND published_at IS NULL
                        """, Timestamp.from(Instant.now().plusSeconds(10)), event.eventId());
                return;
            }
            jdbc.update("""
                    UPDATE blind_box_open_outbox SET published_at = ?
                    WHERE event_id = ? AND published_at IS NULL
                    """, Timestamp.from(Instant.now()), event.eventId());
        }
    }

    private void publishAndConfirm(BlindBoxOpenEvent event) throws Exception {
        var correlation = new CorrelationData();
        rabbit.convertAndSend("", RabbitMQConfig.BLINDBOX_RESULT_GEN_QUEUE, event, correlation);
        var confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 未确认开盒请求成功路由");
        }
    }
}
