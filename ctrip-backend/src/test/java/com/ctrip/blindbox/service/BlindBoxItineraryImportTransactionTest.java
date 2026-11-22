package com.ctrip.blindbox.service;

import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.service.impl.BlindBoxItineraryImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用 H2 验证导入事务：任一 addItem 失败时行程/日程/明细全部回滚。
 */
@SpringBootTest(properties = {
    "app.jwt.secret=test-secret-key-minimum-32-bytes!!",
    "app.cache.bloom.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/import-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BlindBoxItineraryImportTransactionTest {

    @Autowired
    private BlindBoxItineraryImportServiceImpl importService;
    @Autowired
    private BlindBoxOrderMapper orderMapper;
    @Autowired
    private BlindBoxResultMapper resultMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean(name = "destinationBloomFilter")
    private org.redisson.api.RBloomFilter<String> destinationBloomFilter;

    @MockBean(name = "guideBloomFilter")
    private org.redisson.api.RBloomFilter<String> guideBloomFilter;

    @BeforeEach
    void setUp() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("addItem 失败：行程、日程、明细与导入标记全部回滚")
    void addItemFailure_shouldRollbackEverything() {
        BlindBoxOrder order = BlindBoxOrder.builder()
                .userId(1L).templateId(2L).orderNo("BB202611220099")
                .status(OrderStatus.OPENED).build();
        orderMapper.insert(order);

        BlindBoxResult result = BlindBoxResult.builder()
                .orderId(order.getId())
                .destination("杭州").destinationId(3L).theme("文化")
                .resultText("""
                        {"destination":"杭州","destinationId":3,"theme":"文化","days":1,
                         "itinerary":{"day1":{"items":["一个非常非常长的景点名称"]}}}
                        """)
                .build();
        resultMapper.insert(result);

        assertThatThrownBy(() -> importService.importItinerary(1L, "BB202611220099"))
                .isInstanceOf(DataAccessException.class);

        assertThat(count("itinerary")).isZero();
        assertThat(count("itinerary_day")).isZero();
        assertThat(count("itinerary_item")).isZero();
        assertThat(resultMapper.selectById(result.getId()).getImportedItineraryId()).isNull();
    }

    private long count(String table) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }
}
