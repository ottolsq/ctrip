package com.ctrip.blindbox.prize;

import com.ctrip.blindbox.dto.ProbabilityResponse;
import com.ctrip.blindbox.dto.TierProbabilityResponse;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.mapper.DestinationMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 盲盒概率与保底抽取实现。
 */
@Service
public class BlindBoxPrizeServiceImpl implements BlindBoxPrizeService {

    private static final String GUARANTEE_KEY_PREFIX = "blindbox:guarantee:";
    private static final String LOCK_KEY_PREFIX = "lock:blindbox:prize:";
    private static final Duration GUARANTEE_TTL = Duration.ofDays(180);

    private final PrizeProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final DestinationMapper destinationMapper;
    private final DoubleSupplier random;

    public BlindBoxPrizeServiceImpl(PrizeProperties properties,
                                    StringRedisTemplate redisTemplate,
                                    RedissonClient redissonClient,
                                    DestinationMapper destinationMapper) {
        this(properties, redisTemplate, redissonClient, destinationMapper,
                () -> ThreadLocalRandom.current().nextDouble());
    }

    BlindBoxPrizeServiceImpl(PrizeProperties properties,
                             StringRedisTemplate redisTemplate,
                             RedissonClient redissonClient,
                             DestinationMapper destinationMapper,
                             DoubleSupplier random) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.destinationMapper = destinationMapper;
        this.random = random;
    }

    @Override
    public ProbabilityResponse getProbabilities() {
        List<PrizeProperties.TierConfig> tiers = properties.getTiers();
        int totalWeight = tiers.stream()
                .filter(tier -> tier.getTier() != null)
                .mapToInt(PrizeProperties.TierConfig::getWeight)
                .filter(weight -> weight > 0)
                .sum();

        Map<Long, Destination> destinations = loadDestinations(allDestinationIds(tiers));

        List<TierProbabilityResponse> responses = tiers.stream()
                .filter(tier -> tier.getTier() != null)
                .map(tier -> {
                    BigDecimal probability = totalWeight == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(tier.getWeight() * 100.0 / totalWeight)
                                    .setScale(2, RoundingMode.HALF_UP);
                    List<String> names = tier.getDestinationIds().stream()
                            .map(destinations::get)
                            .filter(java.util.Objects::nonNull)
                            .map(Destination::getName)
                            .toList();
                    return new TierProbabilityResponse(tier.getTier().name(), probability, names);
                })
                .toList();

        return new ProbabilityResponse(
                responses, properties.getGuaranteeCount(), properties.getProbabilityVersion());
    }

    @Override
    public DrawnPrize draw(Long userId, Long templateId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + templateId + ":" + userId);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            try {
                return doDraw(userId, templateId);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("抽取被中断");
        }
    }

    private DrawnPrize doDraw(Long userId, Long templateId) {
        String guaranteeKey = GUARANTEE_KEY_PREFIX + templateId + ":" + userId;
        String counter = redisTemplate.opsForValue().get(guaranteeKey);
        int misses = parseCount(counter);
        boolean guaranteed = misses >= properties.getGuaranteeCount();

        List<PrizeProperties.TierConfig> candidates = properties.getTiers().stream()
                .filter(tier -> tier.getTier() != null)
                .filter(tier -> !guaranteed || tier.getTier().atLeastRare())
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException("盲盒概率配置缺失");
        }

        PrizeTier tier = selectTier(random.getAsDouble(), candidates);
        Destination destination = pickDestination(tier);

        if (tier.atLeastRare()) {
            redisTemplate.delete(guaranteeKey);
        } else {
            redisTemplate.opsForValue().increment(guaranteeKey);
            redisTemplate.expire(guaranteeKey, GUARANTEE_TTL);
        }

        return new DrawnPrize(
                tier.name(), destination.getId(), destination.getName(),
                guaranteed, properties.getProbabilityVersion());
    }

    /**
     * 按权重归一化后选择档位；roll ∈ [0,1)。
     */
    static PrizeTier selectTier(double roll, List<PrizeProperties.TierConfig> tiers) {
        int totalWeight = tiers.stream()
                .mapToInt(PrizeProperties.TierConfig::getWeight)
                .filter(weight -> weight > 0)
                .sum();
        if (totalWeight <= 0) {
            throw new BusinessException("盲盒概率配置缺失");
        }
        double target = roll * totalWeight;
        double cumulative = 0;
        for (PrizeProperties.TierConfig tier : tiers) {
            if (tier.getWeight() <= 0) {
                continue;
            }
            cumulative += tier.getWeight();
            if (target < cumulative) {
                return tier.getTier();
            }
        }
        return tiers.getLast().getTier();
    }

    private Destination pickDestination(PrizeTier tier) {
        PrizeProperties.TierConfig config = properties.getTiers().stream()
                .filter(item -> item.getTier() == tier)
                .findFirst()
                .orElseThrow(() -> new BusinessException("盲盒概率配置缺失"));

        List<Destination> destinations = destinationMapper.selectBatchIds(config.getDestinationIds());
        if (destinations == null || destinations.isEmpty()) {
            // 配置池中的目的地被删除时，降级到全部奖品池，避免无法开盒
            destinations = destinationMapper.selectBatchIds(allDestinationIds(properties.getTiers()));
        }
        if (destinations == null || destinations.isEmpty()) {
            throw new BusinessException("盲盒奖品池未配置可用目的地");
        }
        int index = Math.min((int) (random.getAsDouble() * destinations.size()), destinations.size() - 1);
        return destinations.get(index);
    }

    private Set<Long> allDestinationIds(List<PrizeProperties.TierConfig> tiers) {
        return tiers.stream()
                .flatMap(tier -> tier.getDestinationIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, Destination> loadDestinations(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return destinationMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity(), (a, b) -> a));
    }

    private int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
