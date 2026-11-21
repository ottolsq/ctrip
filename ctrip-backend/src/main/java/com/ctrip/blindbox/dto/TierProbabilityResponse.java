package com.ctrip.blindbox.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单个档位的概率公示。
 */
public record TierProbabilityResponse(
        String tier,
        BigDecimal probability,
        List<String> destinations
) {
}
