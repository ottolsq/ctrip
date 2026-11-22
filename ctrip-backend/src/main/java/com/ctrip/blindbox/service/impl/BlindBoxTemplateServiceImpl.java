package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.converter.TemplateConverter;
import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.rule.BlindBoxRuleConfigParser;
import com.ctrip.blindbox.service.BlindBoxTemplateService;
import com.ctrip.blindbox.service.FlashSaleService;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 盲盒模板服务实现。
 */
@Service
public class BlindBoxTemplateServiceImpl implements BlindBoxTemplateService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BlindBoxTemplateMapper templateMapper;
    private final BlindBoxOrderMapper orderMapper;
    private final FlashSaleService flashSaleService;

    public BlindBoxTemplateServiceImpl(BlindBoxTemplateMapper templateMapper,
                                       BlindBoxOrderMapper orderMapper,
                                       FlashSaleService flashSaleService) {
        this.templateMapper = templateMapper;
        this.orderMapper = orderMapper;
        this.flashSaleService = flashSaleService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TemplateResponse> listActiveTemplates(int page, int limit, String type) {
        Page<BlindBoxTemplate> pageObj = new Page<>(page, limit);

        LambdaQueryWrapper<BlindBoxTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxTemplate::getStatus, TemplateStatus.ACTIVE);

        if (type != null && !type.isBlank()) {
            wrapper.eq(BlindBoxTemplate::getType, BlindBoxType.valueOf(type));
        }
        wrapper.orderByDesc(BlindBoxTemplate::getCreatedAt);

        Page<BlindBoxTemplate> result = templateMapper.selectPage(pageObj, wrapper);

        Page<TemplateResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(
                result.getRecords().stream()
                        .map(TemplateConverter::toResponse)
                        .toList()
        );
        return responsePage;
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateDetail(Long id) {
        BlindBoxTemplate template = requireTemplate(id);
        // 仅上架中的模板可查看详情
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ResourceNotFoundException("模板已下架：id=" + id);
        }
        return TemplateConverter.toResponse(template);
    }

    // ========== 后台管理方法（Admin 使用） ==========

    /**
     * 查询所有模板（后台管理），支持类型/状态/关键词筛选。
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TemplateResponse> listAllTemplates(int page, int limit, BlindBoxType type,
                                                   TemplateStatus status, String keyword) {
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        Page<BlindBoxTemplate> pageObj = new Page<>(page, effectiveLimit);
        LambdaQueryWrapper<BlindBoxTemplate> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(BlindBoxTemplate::getType, type);
        }
        if (status != null) {
            wrapper.eq(BlindBoxTemplate::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(BlindBoxTemplate::getName, keyword);
        }
        wrapper.orderByDesc(BlindBoxTemplate::getCreatedAt);

        Page<BlindBoxTemplate> result = templateMapper.selectPage(pageObj, wrapper);
        Page<TemplateResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(
                result.getRecords().stream()
                        .map(TemplateConverter::toResponse)
                        .toList()
        );
        return responsePage;
    }

    /**
     * 创建模板（后台管理）。
     */
    @Override
    @Transactional
    public TemplateResponse createTemplate(BlindBoxTemplate template) {
        validateCreate(template);
        if (template.getType() == BlindBoxType.DAILY) {
            template.setStock(-1);
        }
        templateMapper.insert(template);
        return TemplateConverter.toResponse(template);
    }

    /**
     * 更新模板（后台管理）：仅提交字段参与 UPDATE，避免写回陈旧库存。
     */
    @Override
    @Transactional
    public TemplateResponse updateTemplate(Long id, BlindBoxTemplate updates) {
        BlindBoxTemplate existing = requireTemplate(id);
        BlindBoxType targetType = updates.getType() != null ? updates.getType() : existing.getType();

        if (updates.getName() != null && updates.getName().isBlank()) {
            throw new BusinessException("盲盒名称不能为空");
        }
        if (updates.getPrice() != null && updates.getPrice().signum() <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
        if (updates.getStock() != null
                && targetType == BlindBoxType.LIMITED
                && updates.getStock() < 0) {
            throw new BusinessException("库存不能为负数");
        }

        if (targetType == BlindBoxType.LIMITED) {
            Integer effectiveStock = updates.getStock() != null ? updates.getStock() : existing.getStock();
            if (effectiveStock == null || effectiveStock < 0) {
                throw new BusinessException("限定盲盒库存不能为负数");
            }
            if (updates.getType() == BlindBoxType.LIMITED
                    || updates.getRuleConfig() != null) {
                String effectiveRule = updates.getRuleConfig() != null
                        ? updates.getRuleConfig()
                        : existing.getRuleConfig();
                BlindBoxRuleConfigParser.parse(effectiveRule);
            }
        }

        LambdaUpdateWrapper<BlindBoxTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BlindBoxTemplate::getId, id);
        boolean hasUpdates = false;

        if (updates.getName() != null) {
            wrapper.set(BlindBoxTemplate::getName, updates.getName());
            hasUpdates = true;
        }
        if (updates.getType() != null) {
            wrapper.set(BlindBoxTemplate::getType, updates.getType());
            hasUpdates = true;
        }
        if (updates.getPrice() != null) {
            wrapper.set(BlindBoxTemplate::getPrice, updates.getPrice());
            hasUpdates = true;
        }
        if (targetType == BlindBoxType.DAILY) {
            // 日常盲盒无论提交何种库存都归一到 -1
            wrapper.set(BlindBoxTemplate::getStock, -1);
            hasUpdates = true;
        } else if (updates.getStock() != null) {
            wrapper.set(BlindBoxTemplate::getStock, updates.getStock());
            hasUpdates = true;
        }
        if (updates.getRuleConfig() != null) {
            wrapper.set(BlindBoxTemplate::getRuleConfig, updates.getRuleConfig());
            hasUpdates = true;
        }
        if (updates.getStatus() != null) {
            wrapper.set(BlindBoxTemplate::getStatus, updates.getStatus());
            hasUpdates = true;
        }

        if (!hasUpdates) {
            return TemplateConverter.toResponse(existing);
        }
        if (existing.getVersion() != null) {
            wrapper.eq(BlindBoxTemplate::getVersion, existing.getVersion())
                    .set(BlindBoxTemplate::getVersion, existing.getVersion() + 1);
        }
        if (templateMapper.update(null, wrapper) == 0) {
            throw new BusinessException("盲盒模板已被他人修改，请刷新后重试");
        }
        BlindBoxTemplate refreshed = requireTemplate(id);
        if (refreshed.getType() == BlindBoxType.LIMITED && updates.getStock() != null) {
            flashSaleService.onAdminStockChanged(id, refreshed.getStock());
        }
        return TemplateConverter.toResponse(refreshed);
    }

    /**
     * 删除模板（后台管理）：存在关联订单时拒绝。
     */
    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        requireTemplate(id);
        long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<BlindBoxOrder>()
                        .eq(BlindBoxOrder::getTemplateId, id));
        if (orderCount > 0) {
            throw new BusinessException("存在关联订单，无法删除该模板");
        }
        try {
            templateMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // check-then-delete 窗口内新建了订单，外键阻止删除
            throw new BusinessException("存在关联订单，无法删除该模板");
        }
    }

    private void validateCreate(BlindBoxTemplate template) {
        if (template.getName() == null || template.getName().isBlank()) {
            throw new BusinessException("盲盒名称不能为空");
        }
        if (template.getPrice() == null || template.getPrice().signum() <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
        if (template.getType() == null) {
            throw new BusinessException("盲盒类型不能为空");
        }
        if (template.getType() == BlindBoxType.LIMITED) {
            if (template.getStock() == null || template.getStock() < 1) {
                throw new BusinessException("限定盲盒库存必须大于 0");
            }
            BlindBoxRuleConfigParser.parse(template.getRuleConfig());
        }
    }

    // ========== 私有辅助 ==========

    private BlindBoxTemplate requireTemplate(Long id) {
        BlindBoxTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ResourceNotFoundException("盲盒模板不存在：id=" + id);
        }
        return template;
    }
}
