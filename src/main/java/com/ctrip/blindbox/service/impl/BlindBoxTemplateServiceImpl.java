package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.converter.TemplateConverter;
import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.BlindBoxTemplateService;
import com.ctrip.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 盲盒模板服务实现。
 */
@Service
public class BlindBoxTemplateServiceImpl implements BlindBoxTemplateService {

    private final BlindBoxTemplateMapper templateMapper;

    public BlindBoxTemplateServiceImpl(BlindBoxTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
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
     * 查询所有模板（后台管理，不分页限制）。
     */
    @Transactional(readOnly = true)
    public Page<TemplateResponse> listAllTemplates(int page, int limit) {
        Page<BlindBoxTemplate> pageObj = new Page<>(page, limit);
        LambdaQueryWrapper<BlindBoxTemplate> wrapper = new LambdaQueryWrapper<>();
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
    @Transactional
    public TemplateResponse createTemplate(BlindBoxTemplate template) {
        templateMapper.insert(template);
        return TemplateConverter.toResponse(template);
    }

    /**
     * 更新模板（后台管理）。
     */
    @Transactional
    public TemplateResponse updateTemplate(Long id, BlindBoxTemplate updates) {
        BlindBoxTemplate existing = requireTemplate(id);

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getType() != null) {
            existing.setType(updates.getType());
        }
        if (updates.getPrice() != null) {
            existing.setPrice(updates.getPrice());
        }
        if (updates.getStock() != null) {
            existing.setStock(updates.getStock());
        }
        if (updates.getRuleConfig() != null) {
            existing.setRuleConfig(updates.getRuleConfig());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }

        templateMapper.updateById(existing);
        return TemplateConverter.toResponse(existing);
    }

    /**
     * 删除模板（后台管理）。
     */
    @Transactional
    public void deleteTemplate(Long id) {
        requireTemplate(id);
        templateMapper.deleteById(id);
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
