package com.ctrip.blindbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctrip.blindbox.entity.BlindBoxPreference;
import com.ctrip.blindbox.entity.enums.BudgetLevel;
import com.ctrip.blindbox.mapper.BlindBoxPreferenceMapper;
import com.ctrip.blindbox.service.BlindBoxPreferenceService;
import org.springframework.stereotype.Service;

/**
 * 盲盒预选参数服务实现。
 */
@Service
public class BlindBoxPreferenceServiceImpl implements BlindBoxPreferenceService {

    private final BlindBoxPreferenceMapper preferenceMapper;

    public BlindBoxPreferenceServiceImpl(BlindBoxPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @Override
    public BlindBoxPreference savePreference(Long orderId, String departureCity,
                                             String budgetLevel, String theme) {
        BlindBoxPreference preference = BlindBoxPreference.builder()
                .orderId(orderId)
                .departureCity(departureCity)
                .budgetLevel(BudgetLevel.valueOf(budgetLevel))
                .theme(theme)
                .build();
        preferenceMapper.insert(preference);
        return preference;
    }

    @Override
    public BlindBoxPreference getByOrderId(Long orderId) {
        LambdaQueryWrapper<BlindBoxPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxPreference::getOrderId, orderId);
        return preferenceMapper.selectOne(wrapper);
    }
}
