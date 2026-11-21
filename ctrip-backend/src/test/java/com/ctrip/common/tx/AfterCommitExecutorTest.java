package com.ctrip.common.tx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("事务提交后执行器单元测试")
class AfterCommitExecutorTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("无事务时立即执行")
    void run_withoutTransaction_shouldRunImmediately() {
        AtomicInteger count = new AtomicInteger();

        AfterCommitExecutor.run(count::incrementAndGet);

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("有事务时延迟到 afterCommit 执行")
    void run_withTransaction_shouldDeferUntilAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger count = new AtomicInteger();

        AfterCommitExecutor.run(count::incrementAndGet);
        assertThat(count.get()).isZero();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(count.get()).isEqualTo(1);
    }
}
