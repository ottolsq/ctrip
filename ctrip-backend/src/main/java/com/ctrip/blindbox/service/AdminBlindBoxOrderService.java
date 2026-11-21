package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.AdminOrderDetailResponse;
import com.ctrip.blindbox.dto.AdminOrderResponse;
import com.ctrip.blindbox.entity.enums.OrderStatus;

import java.time.LocalDate;

/**
 * 管理端盲盒订单服务：列表、详情、退款与开盒异常重试。
 */
public interface AdminBlindBoxOrderService {

    Page<AdminOrderResponse> listOrders(int page, int size, String orderNo, Long userId,
                                        OrderStatus status, LocalDate startDate, LocalDate endDate);

    AdminOrderDetailResponse getOrderDetail(String orderNo);

    void refund(String orderNo, String reason, Long operatorId);

    void retryOpen(String orderNo);
}
