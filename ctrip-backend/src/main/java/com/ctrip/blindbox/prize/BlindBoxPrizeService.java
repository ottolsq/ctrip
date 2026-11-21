package com.ctrip.blindbox.prize;

import com.ctrip.blindbox.dto.ProbabilityResponse;

/**
 * 盲盒概率公示与加权抽取。
 */
public interface BlindBoxPrizeService {

    ProbabilityResponse getProbabilities();

    DrawnPrize draw(Long userId, Long templateId);
}
