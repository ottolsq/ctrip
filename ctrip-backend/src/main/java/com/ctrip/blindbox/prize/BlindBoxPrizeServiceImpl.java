package com.ctrip.blindbox.prize;

import com.ctrip.blindbox.dto.ProbabilityResponse;
import com.ctrip.blindbox.dto.TierProbabilityResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.mapper.DestinationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

/**
 * 盲盒概率与保底抽取实现。
 */
@Service
public class BlindBoxPrizeServiceImpl implements BlindBoxPrizeService {

    private static final String GUARANTEE_KEY_PREFIX = "blindbox:guarantee:";
    private static final String DRAW_KEY_PREFIX = "blindbox:draw:";
    private static final String LOCK_KEY_PREFIX = "lock:blindbox:prize:";

    private final PrizeProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final DestinationMapper destinationMapper;
    private final ObjectMapper objectMapper;
    private final DoubleSupplier random;

    @Autowired
    public BlindBoxPrizeServiceImpl(PrizeProperties properties,
                                    StringRedisTemplate redisTemplate,
                                    RedissonClient redissonClient,
                                    DestinationMapper destinationMapper,
                                    ObjectMapper objectMapper) {
        this(properties, redisTemplate, redissonClient, destinationMapper, objectMapper,
                () -> ThreadLocalRandom.current().nextDouble());
    }

    BlindBoxPrizeServiceImpl(PrizeProperties properties,
                             StringRedisTemplate redisTemplate,
                             RedissonClient redissonClient,
                             DestinationMapper destinationMapper,
                             ObjectMapper objectMapper,
                             DoubleSupplier random) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.destinationMapper = destinationMapper;
        this.objectMapper = objectMapper;
        this.random = random;
    }

    @Override
    public ProbabilityResponse getProbabilities() {
        List<PrizeProperties.TierConfig> available = availableTiers(false);
        int totalWeight = available.stream()
                .mapToInt(PrizeProperties.TierConfig::getWeight)
                .sum();

        List<TierProbabilityResponse> responses = available.stream()
                .map(tier -> {
                    List<Destination> destinations = destinationsFor(tier);
                    BigDecimal probability = totalWeight == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(tier.getWeight() * 100.0 / totalWeight)
                                    .setScale(2, RoundingMode.HALF_UP);
                    return new TierProbabilityResponse(
                            tier.getTier().name(), probability,
                            destinations.stream().map(Destination::getName).toList());
                })
                .toList();

        return new ProbabilityResponse(
                responses, properties.getGuaranteeCount(), properties.getProbabilityVersion());
    }

    @Override
    public DrawnPrize draw(BlindBoxOrder order) {
        RLock lock = redissonClient.getLock(
                LOCK_KEY_PREFIX + order.getTemplateId() + ":" + order.getUserId());
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            try {
                String cacheKey = DRAW_KEY_PREFIX + order.getId();
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, DrawnPrize.class);
                }

                DrawnPrize prize = doDraw(order.getUserId(), order.getTemplateId());
                redisTemplate.opsForValue().set(
                        cacheKey, objectMapper.writeValueAsString(prize), 7, TimeUnit.DAYS);
                return prize;
            } catch (JsonProcessingException e) {
                throw new BusinessException("抽取结果缓存失败");
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

        List<PrizeProperties.TierConfig> candidates = availableTiers(guaranteed);
        if (candidates.isEmpty()) {
            throw new BusinessException("盲盒奖品池未配置可用目的地");
        }

        PrizeTier tier = selectTier(random.getAsDouble(), candidates);
        PrizeProperties.TierConfig selected = candidates.stream()
                .filter(item -> item.getTier() == tier)
                .findFirst()
                .orElseThrow(() -> new BusinessException("盲盒概率配置缺失"));
        List<Destination> destinations = destinationsFor(selected);
        int index = Math.min(
                (int) (random.getAsDouble() * destinations.size()), destinations.size() - 1);
        Destination destination = destinations.get(index);

        if (tier.atLeastRare()) {
            redisTemplate.delete(guaranteeKey);
        } else {
            Long next = redisTemplate.opsForValue().increment(guaranteeKey);
            if (next != null) {
                // 使用 SETEX 写回并续期，避免部分 Redis 客户端 pExpire 实现递归
                redisTemplate.opsForValue().set(
                        guaranteeKey, String.valueOf(next), 180, TimeUnit.DAYS);
            }
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

    /**
     * 仅保留配置了有效目的地的档位；公开概率与实际抽取共用，保证口径一致。
     */
    private List<PrizeProperties.TierConfig> availableTiers(boolean rareOnly) {
        List<PrizeProperties.TierConfig> available = new ArrayList<>();
        for (PrizeProperties.TierConfig tier : properties.getTiers()) {
            if (tier.getTier() == null || tier.getWeight() <= 0) {
                continue;
            }
            if (rareOnly && !tier.getTier().atLeastRare()) {
                continue;
            }
            if (!destinationsFor(tier).isEmpty()) {
                available.add(tier);
            }
        }
        return available;
    }

    private List<Destination> destinationsFor(PrizeProperties.TierConfig tier) {
        List<Long> ids = tier.getDestinationIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Destination> destinations = destinationMapper.selectBatchIds(ids);
        return destinations == null ? List.of() : destinations;
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
