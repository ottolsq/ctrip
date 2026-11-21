package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.DrawRecordResponse;
import com.ctrip.blindbox.entity.BlindBoxDrawRecord;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.mapper.BlindBoxDrawRecordMapper;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.prize.DrawnPrize;
import com.ctrip.blindbox.prize.PrizeTier;
import com.ctrip.blindbox.service.DrawRecordService;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抽取记录服务实现。
 */
@Service
public class DrawRecordServiceImpl implements DrawRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BlindBoxDrawRecordMapper recordMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final BlindBoxTemplateMapper templateMapper;
    private final UserMapper userMapper;

    public DrawRecordServiceImpl(BlindBoxDrawRecordMapper recordMapper,
                                 BlindBoxOrderMapper orderMapper,
                                 BlindBoxTemplateMapper templateMapper,
                                 UserMapper userMapper) {
        this.recordMapper = recordMapper;
        this.orderMapper = orderMapper;
        this.templateMapper = templateMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void record(BlindBoxOrder order, DrawnPrize prize) {
        recordMapper.insert(BlindBoxDrawRecord.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .templateId(order.getTemplateId())
                .tier(PrizeTier.valueOf(prize.tier()))
                .destinationId(prize.destinationId())
                .destinationName(prize.destinationName())
                .guaranteed(prize.guaranteed())
                .probabilityVersion(prize.probabilityVersion())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DrawRecordResponse> listMine(Long userId, int page, int size) {
        LambdaQueryWrapper<BlindBoxDrawRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxDrawRecord::getUserId, userId)
                .orderByDesc(BlindBoxDrawRecord::getCreatedAt);
        return queryPage(page, size, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DrawRecordResponse> listAll(int page, int size, Long userId, Long templateId,
                                            PrizeTier tier, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BlindBoxDrawRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(BlindBoxDrawRecord::getUserId, userId);
        }
        if (templateId != null) {
            wrapper.eq(BlindBoxDrawRecord::getTemplateId, templateId);
        }
        if (tier != null) {
            wrapper.eq(BlindBoxDrawRecord::getTier, tier);
        }
        if (startDate != null) {
            wrapper.ge(BlindBoxDrawRecord::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(BlindBoxDrawRecord::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(BlindBoxDrawRecord::getCreatedAt);
        return queryPage(page, size, wrapper);
    }

    private Page<DrawRecordResponse> queryPage(int page, int size,
                                               LambdaQueryWrapper<BlindBoxDrawRecord> wrapper) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<BlindBoxDrawRecord> result = recordMapper.selectPage(
                new Page<>(page, effectiveSize), wrapper);
        List<BlindBoxDrawRecord> records = result.getRecords();

        Map<Long, String> orderNos = loadOrderNos(ids(records, BlindBoxDrawRecord::getOrderId));
        Map<Long, String> templateNames = loadTemplateNames(ids(records, BlindBoxDrawRecord::getTemplateId));
        Map<Long, String> userNames = loadUserNames(ids(records, BlindBoxDrawRecord::getUserId));

        Page<DrawRecordResponse> response = new Page<>(
                result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(records.stream()
                .map(record -> new DrawRecordResponse(
                        record.getId(),
                        record.getOrderId(),
                        orderNos.get(record.getOrderId()),
                        record.getUserId(),
                        userNames.get(record.getUserId()),
                        record.getTemplateId(),
                        templateNames.get(record.getTemplateId()),
                        record.getTier() != null ? record.getTier().name() : null,
                        record.getDestinationId(),
                        record.getDestinationName(),
                        Boolean.TRUE.equals(record.getGuaranteed()),
                        record.getProbabilityVersion(),
                        record.getCreatedAt()))
                .toList());
        return response;
    }

    private Set<Long> ids(List<BlindBoxDrawRecord> records,
                          Function<BlindBoxDrawRecord, Long> mapper) {
        return records.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> loadOrderNos(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BlindBoxOrder::getId, BlindBoxOrder::getOrderNo, (a, b) -> a));
    }

    private Map<Long, String> loadTemplateNames(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return templateMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BlindBoxTemplate::getId, BlindBoxTemplate::getName, (a, b) -> a));
    }

    private Map<Long, String> loadUserNames(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }
}
