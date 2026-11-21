package com.ctrip.blindbox.prize;

import com.ctrip.blindbox.dto.ProbabilityResponse;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.mapper.DestinationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("盲盒概率与保底服务单元测试")
class BlindBoxPrizeServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private DestinationMapper destinationMapper;

    private PrizeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PrizeProperties();
        properties.setGuaranteeCount(10);
        properties.setProbabilityVersion("v1");
        properties.setTiers(List.of(
                tier(PrizeTier.COMMON, 70, List.of(1L, 2L, 3L)),
                tier(PrizeTier.RARE, 25, List.of(4L)),
                tier(PrizeTier.LEGENDARY, 5, List.of(5L))
        ));
    }

    private PrizeProperties.TierConfig tier(PrizeTier tier, int weight, List<Long> ids) {
        PrizeProperties.TierConfig config = new PrizeProperties.TierConfig();
        config.setTier(tier);
        config.setWeight(weight);
        config.setDestinationIds(ids);
        return config;
    }

    private BlindBoxPrizeServiceImpl service(double roll) {
        return new BlindBoxPrizeServiceImpl(
                properties, redisTemplate, redissonClient, destinationMapper, () -> roll);
    }

    private void mockLock() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    private Destination destination(Long id, String name) {
        return Destination.builder().id(id).name(name).build();
    }

    @Test
    @DisplayName("权重边界：0/70/95 分界正确")
    void selectTier_boundaries() {
        List<PrizeProperties.TierConfig> tiers = properties.getTiers();

        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.0, tiers)).isEqualTo(PrizeTier.COMMON);
        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.6999, tiers)).isEqualTo(PrizeTier.COMMON);
        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.7, tiers)).isEqualTo(PrizeTier.RARE);
        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.9499, tiers)).isEqualTo(PrizeTier.RARE);
        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.95, tiers)).isEqualTo(PrizeTier.LEGENDARY);
        assertThat(BlindBoxPrizeServiceImpl.selectTier(0.9999, tiers)).isEqualTo(PrizeTier.LEGENDARY);
    }

    @Test
    @DisplayName("公示概率：归一化两位小数并过滤不存在的目的地")
    void getProbabilities_shouldNormalizeAndExcludeMissingDestinations() {
        when(destinationMapper.selectBatchIds(any())).thenReturn(List.of(
                destination(1L, "杭州"), destination(2L, "北京"), destination(5L, "巴厘岛")));

        ProbabilityResponse response = service(0.0).getProbabilities();

        assertThat(response.guaranteeCount()).isEqualTo(10);
        assertThat(response.probabilityVersion()).isEqualTo("v1");
        assertThat(response.tiers()).hasSize(3);
        assertThat(response.tiers().get(0).tier()).isEqualTo("COMMON");
        assertThat(response.tiers().get(0).probability()).isEqualByComparingTo("70.00");
        assertThat(response.tiers().get(0).destinations()).containsExactly("杭州", "北京");
        assertThat(response.tiers().get(1).probability()).isEqualByComparingTo("25.00");
        assertThat(response.tiers().get(1).destinations()).isEmpty();
        assertThat(response.tiers().get(2).probability()).isEqualByComparingTo("5.00");
        assertThat(response.tiers().get(2).destinations()).containsExactly("巴厘岛");
    }

    @Test
    @DisplayName("保底：连续 10 次未中后强制稀有+并清零")
    void draw_guaranteeAfterTenMisses_shouldForceRareOrLegendary() throws InterruptedException {
        mockLock();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blindbox:guarantee:9:1")).thenReturn("10");
        when(destinationMapper.selectBatchIds(any())).thenReturn(List.of(
                destination(4L, "京都"), destination(5L, "巴厘岛")));

        DrawnPrize prize = service(0.0).draw(1L, 9L);

        assertThat(prize.tier()).isEqualTo("RARE");
        assertThat(prize.destinationId()).isEqualTo(4L);
        assertThat(prize.destinationName()).isEqualTo("京都");
        assertThat(prize.guaranteed()).isTrue();
        verify(redisTemplate).delete("blindbox:guarantee:9:1");
    }

    @Test
    @DisplayName("命中稀有+：计数清零")
    void draw_rareHit_shouldResetCounter() throws InterruptedException {
        mockLock();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blindbox:guarantee:9:1")).thenReturn("3");
        when(destinationMapper.selectBatchIds(any())).thenReturn(List.of(destination(5L, "巴厘岛")));

        DrawnPrize prize = service(0.99).draw(1L, 9L);

        assertThat(prize.tier()).isEqualTo("LEGENDARY");
        assertThat(prize.guaranteed()).isFalse();
        verify(redisTemplate).delete("blindbox:guarantee:9:1");
    }

    @Test
    @DisplayName("未中稀有：计数 +1 并设置 TTL")
    void draw_common_shouldIncrementCounterWithTtl() throws InterruptedException {
        mockLock();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blindbox:guarantee:9:1")).thenReturn("3");
        when(valueOperations.increment("blindbox:guarantee:9:1")).thenReturn(4L);
        when(destinationMapper.selectBatchIds(any())).thenReturn(List.of(
                destination(1L, "杭州"), destination(2L, "北京")));

        DrawnPrize prize = service(0.1).draw(1L, 9L);

        assertThat(prize.tier()).isEqualTo("COMMON");
        assertThat(prize.guaranteed()).isFalse();
        verify(valueOperations).increment("blindbox:guarantee:9:1");
        verify(valueOperations).set("blindbox:guarantee:9:1", "4", 180, TimeUnit.DAYS);
        verify(redisTemplate, never()).delete(anyString());
    }
}
