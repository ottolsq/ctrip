package com.ctrip.blindbox.dto;

/**
 * 管理端盲盒订单详情：订单信息 + 已开盒结果（未开盒时为 null）。
 */
public record AdminOrderDetailResponse(
        AdminOrderResponse order,
        ResultResponse result
) {
}
