package com.ctrip.blindbox.service;

import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.blindbox.entity.BlindBoxPreference;

/**
 * 盲盒方案生成服务接口。
 *
 * <p>MVP 阶段返回基于规则的 mock 方案，后续接入 AI API。
 */
public interface BlindBoxSchemeService {

    /**
     * 根据预选参数生成盲盒旅行方案。
     *
     * <p>MVP 返回基于规则的 mock 方案，后续对接真实大模型 API。
     *
     * @param preference 用户预选参数
     * @return 生成的旅行方案
     */
    SchemeResponse generateScheme(BlindBoxPreference preference);
}
