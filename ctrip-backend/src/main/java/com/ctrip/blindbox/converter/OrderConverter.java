package com.ctrip.blindbox.converter;

import com.ctrip.blindbox.dto.OrderResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;

/**
 * 盲盒订单 Entity ↔ DTO 转换器。
 *
 * 无状态工具类，不注册为 Spring Bean。
 */
public class OrderConverter {

    private OrderConverter() {
    }

    /**
     * Entity → Response。
     *
     * @param order    订单实体
     * @param template 关联的模板（用于获取名称）
     */
    public static OrderResponse toResponse(BlindBoxOrder order, BlindBoxTemplate template) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getTemplateId(),
                template != null ? template.getName() : null,
                order.getPayAmount(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getExpireAt(),
                order.getPayTime(),
                order.getCreatedAt()
        );
    }

    /**
     * Entity → Response（不含模板信息，用于列表等场景）。
     */
    public static OrderResponse toResponse(BlindBoxOrder order) {
        return toResponse(order, null);
    }
}
