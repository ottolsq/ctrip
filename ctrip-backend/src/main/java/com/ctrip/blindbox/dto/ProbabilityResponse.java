package com.ctrip.blindbox.dto;

import java.util.List;

/**
 * 盲盒概率公示响应。
 */
public record ProbabilityResponse(
        List<TierProbabilityResponse> tiers,
        int guaranteeCount,
        String probabilityVersion
) {
}
