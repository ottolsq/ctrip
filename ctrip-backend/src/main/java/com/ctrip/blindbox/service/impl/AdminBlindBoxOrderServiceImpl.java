package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.converter.ResultConverter;
import com.ctrip.blindbox.dto.AdminOrderDetailResponse;
import com.ctrip.blindbox.dto.AdminOrderResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.AdminBlindBoxOrderService;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.common.tx.AfterCommitExecutor;
import com.ctrip.messaging.publisher.EventPublisher;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端盲盒订单服务实现。
 */
@Service
public class AdminBlindBoxOrderServiceImpl implements AdminBlindBoxOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminBlindBoxOrderServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxTemplateMapper templateMapper;
    private final BlindBoxResultMapper resultMapper;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    public AdminBlindBoxOrderServiceImpl(BlindBoxOrderMapper orderMapper,
                                         BlindBoxTemplateMapper templateMapper,
                                         BlindBoxResultMapper resultMapper,
                                         UserMapper userMapper,
                                         EventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.templateMapper = templateMapper;
        this.resultMapper = resultMapper;
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> listOrders(int page, int size, String orderNo, Long userId,
                                               OrderStatus status, LocalDate startDate, LocalDate endDate) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<BlindBoxOrder> pageObj = new Page<>(page, effectiveSize);

        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(BlindBoxOrder::getOrderNo, orderNo);
        }
        if (userId != null) {
            wrapper.eq(BlindBoxOrder::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(BlindBoxOrder::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(BlindBoxOrder::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(BlindBoxOrder::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(BlindBoxOrder::getCreatedAt);

        Page<BlindBoxOrder> result = orderMapper.selectPage(pageObj, wrapper);
        List<BlindBoxOrder> orders = result.getRecords();

        Map<Long, String> userNames = loadUserNames(orders);
        Map<Long, String> templateNames = loadTemplateNames(orders);

        Page<AdminOrderResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(orders.stream()
                .map(order -> toResponse(order,
                        userNames.get(order.getUserId()),
                        templateNames.get(order.getTemplateId())))
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo);
        String userName = order.getUserId() == null ? null : userName(order.getUserId());
        String templateName = order.getTemplateId() == null ? null : templateName(order.getTemplateId());

        BlindBoxResult result = resultMapper.selectOne(
                new LambdaQueryWrapper<BlindBoxResult>()
                        .eq(BlindBoxResult::getOrderId, order.getId()));

        return new AdminOrderDetailResponse(
                toResponse(order, userName, templateName),
                result != null ? ResultConverter.toResponse(result, order.getOrderNo()) : null);
    }

    @Override
    @Transactional
    public void refund(String orderNo, String reason, Long operatorId) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("退款原因不能为空");
        }
        BlindBoxOrder order = requireOrder(orderNo);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.OPENED) {
            throw new BusinessException("仅已支付或已开盒订单可退款");
        }

        LocalDateTime refundedAt = LocalDateTime.now();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                .eq(BlindBoxOrder::getId, order.getId())
                .in(BlindBoxOrder::getStatus, OrderStatus.PAID, OrderStatus.OPENED)
                .set(BlindBoxOrder::getStatus, OrderStatus.REFUNDED)
                .set(BlindBoxOrder::getRefundReason, reason)
                .set(BlindBoxOrder::getRefundedAt, refundedAt));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化，退款失败");
        }

        // 库存与退款同事务恢复：不依赖 MQ 可用性，避免事件丢失导致永久少库存
        BlindBoxTemplate template = templateMapper.selectById(order.getTemplateId());
        if (template != null && template.getType() == BlindBoxType.LIMITED) {
            templateMapper.incrementStock(template.getId());
        }

        AfterCommitExecutor.run(() -> {
            try {
                eventPublisher.publishOrderStatusEvent(
                        "ORDER_REFUNDED", order.getId(), order.getOrderNo(), order.getUserId(),
                        order.getTemplateId(), order.getPayAmount(), order.getPayMethod(),
                        Map.of("reason", reason, "operatorId", String.valueOf(operatorId)));
            } catch (RuntimeException e) {
                // 退款与库存已提交，事件仅用于下游通知/统计；不能让接口误报失败
                log.error("退款事件发布失败: orderNo={}, templateId={}",
                        order.getOrderNo(), order.getTemplateId(), e);
            }
        });
    }

    @Override
    @Transactional
    public void retryOpen(String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo);
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException("仅生成中的订单可重试开盒");
        }

        AfterCommitExecutor.run(() -> eventPublisher.publishBlindBoxOpenEvent(
                "BLINDBOX_OPEN_REQUESTED", order.getId(), order.getUserId(), null, null));
    }

    private BlindBoxOrder requireOrder(String orderNo) {
        BlindBoxOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<BlindBoxOrder>()
                        .eq(BlindBoxOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在：" + orderNo);
        }
        return order;
    }

    private String userName(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }

    private String templateName(Long templateId) {
        BlindBoxTemplate template = templateMapper.selectById(templateId);
        return template != null ? template.getName() : null;
    }

    private Map<Long, String> loadUserNames(List<BlindBoxOrder> orders) {
        Set<Long> ids = orders.stream()
                .map(BlindBoxOrder::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private Map<Long, String> loadTemplateNames(List<BlindBoxOrder> orders) {
        Set<Long> ids = orders.stream()
                .map(BlindBoxOrder::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return templateMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BlindBoxTemplate::getId, BlindBoxTemplate::getName, (a, b) -> a));
    }

    private AdminOrderResponse toResponse(BlindBoxOrder order, String userName, String templateName) {
        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                userName,
                order.getTemplateId(),
                templateName,
                order.getPayAmount(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getPayMethod(),
                order.getPayTime(),
                order.getExpireAt(),
                order.getCreatedAt(),
                order.getRefundReason(),
                order.getRefundedAt()
        );
    }
}
