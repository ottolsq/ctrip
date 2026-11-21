package com.ctrip.blindbox.prize;

/**
 * 单次抽取结果。
 */
public record DrawnPrize(
        String tier,
        Long destinationId,
        String destinationName,
        boolean guaranteed,
        String probabilityVersion
) {
}
