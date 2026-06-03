package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ctrip.blindbox.converter.ResultConverter;
import com.ctrip.blindbox.dto.ResultResponse;
import com.ctrip.blindbox.dto.ShareResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxResult;
import com.ctrip.blindbox.entity.enums.OrderStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxResultMapper;
import com.ctrip.blindbox.service.BlindBoxResultService;
import com.ctrip.common.exception.ForbiddenException;
import com.ctrip.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 盲盒结果服务实现。
 */
@Service
public class BlindBoxResultServiceImpl implements BlindBoxResultService {

    private static final String BASE62_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHARE_CODE_LENGTH = 6;
    private static final long SHARE_EXPIRY_DAYS = 7;

    private final BlindBoxResultMapper resultMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final SecureRandom random = new SecureRandom();

    public BlindBoxResultServiceImpl(BlindBoxResultMapper resultMapper,
                                     BlindBoxOrderMapper orderMapper) {
        this.resultMapper = resultMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultResponse getResult(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);

        // 仅已开盒的订单可查看结果
        if (order.getStatus() != OrderStatus.OPENED) {
            throw new ForbiddenException("订单尚未开盒，无法查看结果");
        }

        BlindBoxResult result = requireResultByOrderId(order.getId());
        return ResultConverter.toResponse(result, orderNo);
    }

    @Override
    @Transactional
    public ShareResponse generateShareLink(Long userId, String orderNo) {
        BlindBoxOrder order = requireOrder(orderNo, userId);

        // 仅已开盒的订单可分享
        if (order.getStatus() != OrderStatus.OPENED) {
            throw new ForbiddenException("订单尚未开盒，无法分享");
        }

        BlindBoxResult result = requireResultByOrderId(order.getId());

        // 若已分享过则复用已有 shareCode
        String shareCode = result.getShareCode();
        LocalDateTime shareExpiresAt = result.getShareExpiresAt();

        if (shareCode == null || shareExpiresAt == null || shareExpiresAt.isBefore(LocalDateTime.now())) {
            // 生成新的分享码
            shareCode = generateShareCode();
            shareExpiresAt = LocalDateTime.now().plusDays(SHARE_EXPIRY_DAYS);

            resultMapper.update(null,
                    new LambdaUpdateWrapper<BlindBoxResult>()
                            .eq(BlindBoxResult::getId, result.getId())
                            .set(BlindBoxResult::getShareCode, shareCode)
                            .set(BlindBoxResult::getShareExpiresAt, shareExpiresAt));
        }

        String shareUrl = "/api/v1/blind-box/share/" + shareCode;
        return new ShareResponse(shareCode, shareUrl, shareExpiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultResponse getSharedResult(String shareCode) {
        LambdaQueryWrapper<BlindBoxResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxResult::getShareCode, shareCode);
        BlindBoxResult result = resultMapper.selectOne(wrapper);

        if (result == null) {
            throw new ResourceNotFoundException("分享链接无效");
        }

        // 检查过期
        if (result.getShareExpiresAt() != null
                && result.getShareExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("分享链接已过期");
        }

        // 通过 orderId 查找 orderNo
        BlindBoxOrder order = orderMapper.selectById(result.getOrderId());
        String orderNo = order != null ? order.getOrderNo() : null;

        return ResultConverter.toResponse(result, orderNo);
    }

    // ========== 私有辅助 ==========

    private BlindBoxOrder requireOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<BlindBoxOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxOrder::getOrderNo, orderNo);
        BlindBoxOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new ResourceNotFoundException("订单不存在：orderNo=" + orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权查看他人订单");
        }
        return order;
    }

    private BlindBoxResult requireResultByOrderId(Long orderId) {
        LambdaQueryWrapper<BlindBoxResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxResult::getOrderId, orderId);
        BlindBoxResult result = resultMapper.selectOne(wrapper);

        if (result == null) {
            throw new ResourceNotFoundException("盲盒结果不存在：orderId=" + orderId);
        }
        return result;
    }

    /**
     * 生成 6 位 Base62 分享短码，含防冲突检查。
     */
    private String generateShareCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(SHARE_CODE_LENGTH);
            for (int j = 0; j < SHARE_CODE_LENGTH; j++) {
                sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
            }
            String code = sb.toString();
            LambdaQueryWrapper<BlindBoxResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlindBoxResult::getShareCode, code);
            if (resultMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        throw new RuntimeException("生成分享码失败，请重试");
    }
}
