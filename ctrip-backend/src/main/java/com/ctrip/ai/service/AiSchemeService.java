package com.ctrip.ai.service;

import com.ctrip.ai.dto.AiSchemeRequest;
import com.ctrip.ai.dto.SchemeOutput;
import com.ctrip.ai.exception.AiServiceException;

/**
 * AI 旅行方案生成服务接口。
 *
 * <p>业务模块（盲盒、行程规划等）依赖此接口，不直接依赖 AI 平台 SDK。
 * 便于未来切换模型供应商（Claude、OpenAI 等），业务模块零改动。
 */
public interface AiSchemeService {

    /**
     * 根据用户偏好生成旅行方案。
     *
     * @param request 请求参数（出发城市、预算、主题等）
     * @return 生成的旅行方案
     * @throws AiServiceException AI 调用失败时抛出
     */
    SchemeOutput generate(AiSchemeRequest request);
}
