package com.ctrip.blindbox.messaging;

import com.ctrip.config.RabbitMQConfig;
import com.ctrip.messaging.event.BlindBoxOpenEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OpenRequestOutboxTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private RabbitTemplate rabbit;
    private OpenRequestOutbox outbox;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new FileSystemResource("docs/SQL/ctrip_blindbox_open_outbox.sql"))
                .execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        rabbit = mock(RabbitTemplate.class);
        outbox = new OpenRequestOutbox(jdbc, rabbit);
    }

    @Test
    void rolledBackRequestCannotBeDispatched() {
        transaction.executeWithoutResult(status -> {
            outbox.enqueue(7L, 8L);
            status.setRollbackOnly();
        });

        outbox.dispatch();

        assertThat(count("SELECT COUNT(*) FROM blind_box_open_outbox")).isZero();
        verifyNoInteractions(rabbit);
    }

    @Test
    void brokerFailureNackAndUnroutableAckStayPendingUntilSuccessfulRetry() {
        transaction.executeWithoutResult(status -> outbox.enqueue(7L, 8L));
        List<BlindBoxOpenEvent> attempts = new ArrayList<>();
        doAnswer(invocation -> {
            BlindBoxOpenEvent event = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            attempts.add(event);
            if (attempts.size() == 1) {
                throw new AmqpException("broker down");
            }
            if (attempts.size() == 3) {
                correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312, "NO_ROUTE",
                        "", RabbitMQConfig.BLINDBOX_RESULT_GEN_QUEUE));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(attempts.size() != 2, null));
            return null;
        }).when(rabbit).convertAndSend(eq(""), eq(RabbitMQConfig.BLINDBOX_RESULT_GEN_QUEUE),
                any(BlindBoxOpenEvent.class), any(CorrelationData.class));

        for (int expected = 1; expected <= 3; expected++) {
            outbox.dispatch();
            assertThat(attempts).hasSize(expected);
            assertThat(count("SELECT COUNT(*) FROM blind_box_open_outbox WHERE published_at IS NULL")).isOne();
            outbox.dispatch();
            assertThat(attempts).hasSize(expected);
            jdbc.update("UPDATE blind_box_open_outbox SET next_attempt_at = created_at");
        }
        outbox.dispatch();
        outbox.dispatch();

        assertThat(count("SELECT COUNT(*) FROM blind_box_open_outbox WHERE published_at IS NOT NULL")).isOne();
        assertThat(attempts).hasSize(4).allMatch(event -> event.equals(attempts.getFirst()));
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
