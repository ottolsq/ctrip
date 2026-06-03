package com.ctrip.blindbox.converter;

import com.ctrip.blindbox.dto.ResultResponse;
import com.ctrip.blindbox.entity.BlindBoxResult;

/**
 * 盲盒结果 Entity ↔ DTO 转换器。
 *
 * 无状态工具类，不注册为 Spring Bean。
 */
public class ResultConverter {

    private ResultConverter() {
    }

    /**
     * Entity → Response。
     *
     * @param result  结果实体
     * @param orderNo 关联的订单编号（从外部传入，因为结果表只存 orderId）
     */
    public static ResultResponse toResponse(BlindBoxResult result, String orderNo) {
        return new ResultResponse(
                result.getId(),
                orderNo,
                result.getDestination(),
                result.getDestinationId(),
                result.getTheme(),
                result.getResultText(),
                result.getShareCode(),
                result.getOpenedAt(),
                result.getCreatedAt()
        );
    }
}
