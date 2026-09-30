package com.ctrip.ai.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.ctrip.ai.dto.AiSchemeRequest;
import com.ctrip.ai.dto.AiSchemeResponse;
import com.ctrip.ai.dto.SchemeOutput;
import com.ctrip.ai.exception.AiServiceException;
import com.ctrip.ai.prompt.PromptBuilder;
import com.ctrip.ai.prompt.SystemPrompts;
import com.ctrip.ai.service.AiSchemeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope（通义千问）AI 方案生成服务实现。
 *
 * <p>调用阿里云百炼平台的 qwen-plus 模型生成旅行方案，
 * 将 AI 返回的 JSON 解析为标准化 {@link SchemeOutput}。
 *
 * <p>调用流程：
 * <pre>
 * AiSchemeRequest → PromptBuilder 构建 Prompt
 *                → DashScope SDK 调用 qwen-plus
 *                → 清理 markdown 包裹
 *                → Jackson 解析 JSON → AiSchemeResponse
 *                → 转换为 SchemeOutput → 返回给调用方
 * </pre>
 */
@Service
public class DashScopeAiSchemeService implements AiSchemeService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeAiSchemeService.class);

    /** DashScope API key，通过 application.properties 注入。 */
    @Value("${app.ai.dashscope.api-key}")
    private String apiKey;

    /** 使用的模型名称，默认 qwen-plus。 */
    @Value("${app.ai.dashscope.model:qwen-plus}")
    private String model;

    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public DashScopeAiSchemeService(PromptBuilder promptBuilder, ObjectMapper objectMapper) {
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public SchemeOutput generate(AiSchemeRequest request) {
        // 1. 构建 User Prompt
        String userPrompt = promptBuilder.build(request);

        // 2. 调用 DashScope（使用 Builder 模式构造 Message）
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(
                        Message.builder()
                                .role(Role.SYSTEM.getValue())
                                .content(SystemPrompts.TRAVEL_PLANNER)
                                .build(),
                        Message.builder()
                                .role(Role.USER.getValue())
                                .content(userPrompt)
                                .build()))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(0.7f)
                .maxTokens(4096)
                .build();

        GenerationResult result;
        try {
            result = new Generation().call(param);
        } catch (Exception e) {
            throw new AiServiceException("DashScope API 调用失败", e);
        }

        // 3. 提取 AI 响应文本
        String content = extractContent(result);
        if (content == null || content.isBlank()) {
            throw new AiServiceException("AI 返回的响应为空");
        }

        log.debug("AI 原始响应: {}", content);

        // 4. 清理 markdown 代码块包裹（AI 常返回 ```json ... ```）
        content = stripMarkdownJson(content);

        // 5. 解析 JSON 为中间 DTO
        AiSchemeResponse aiResponse;
        try {
            aiResponse = objectMapper.readValue(content, AiSchemeResponse.class);
        } catch (Exception e) {
            log.error("AI JSON 解析失败, content={}", content);
            throw new AiServiceException("AI 返回的 JSON 格式不正确", e);
        }

        // 6. 转换为标准化输出
        return convert(aiResponse, request);
    }

    /**
     * 从 DashScope 响应中提取 AI 生成的文本内容。
     */
    private String extractContent(GenerationResult result) {
        if (result == null || result.getOutput() == null
                || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            return null;
        }
        Message message = result.getOutput().getChoices().get(0).getMessage();
        return message != null ? message.getContent() : null;
    }

    /**
     * 清理 AI 返回的 markdown 代码块包裹。
     *
     * <p>AI 经常返回 ```json { ... } ``` 格式，
     * 需要剥离外层才能直接解析 JSON。
     */
    private String stripMarkdownJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            int end = trimmed.indexOf("```", 7);
            if (end > 0) {
                return trimmed.substring(7, end).trim();
            }
        } else if (trimmed.startsWith("```")) {
            int end = trimmed.indexOf("```", 3);
            if (end > 0) {
                return trimmed.substring(3, end).trim();
            }
        }
        return trimmed;
    }

    /**
     * 将 AI 原始响应转换为标准化的 SchemeOutput。
     */
    private SchemeOutput convert(AiSchemeResponse ai, AiSchemeRequest request) {
        // itinerary: Map<String, AiSchemeResponse.DayPlan> → Map<String, SchemeOutput.DayPlan>
        Map<String, SchemeOutput.DayPlan> itinerary = new LinkedHashMap<>();
        if (ai.itinerary() != null) {
            ai.itinerary().forEach((dayKey, dayPlan) -> {
                itinerary.put(dayKey, new SchemeOutput.DayPlan(dayPlan.title(), dayPlan.activities()));
            });
        }

        return new SchemeOutput(
                ai.destination(),
                resolveDestinationId(ai.destination()),
                ai.theme(),
                ai.days(),
                itinerary,
                ai.hotel() != null
                        ? new SchemeOutput.HotelInfo(ai.hotel().name(), ai.hotel().address(), ai.hotel().rating())
                        : null,
                ai.transport() != null
                        ? new SchemeOutput.TransportInfo(ai.transport().type(), ai.transport().departureTime(), ai.transport().description())
                        : null,
                ai.budget() != null
                        ? new SchemeOutput.BudgetInfo(ai.budget().total(), ai.budget().transport(), ai.budget().hotel(), ai.budget().food(), ai.budget().tickets())
                        : null
        );
    }

    /**
     * 根据目的地名称生成一个伪唯一 ID（用于前端展示，非严格数据库 FK）。
     *
     * <p>实际生产环境中应通过 destination 表查询真实 ID。
     */
    private Long resolveDestinationId(String destination) {
        if (destination == null) {
            return null;
        }
        return (long) Math.abs(destination.hashCode()) % 1000;
    }
}
