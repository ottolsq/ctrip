package com.ctrip.blindbox.prize;

import com.ctrip.blindbox.dto.ProbabilityResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;

/**
 * 盲盒概率公示与加权抽取。
 */
public interface BlindBoxPrizeService {

    ProbabilityResponse getProbabilities();

    /**
     * 按订单抽取奖品；同一订单重复调用返回同一结果（Redis 幂等缓存）。
     */
    DrawnPrize draw(BlindBoxOrder order);
}
