package com.ctrip.blindbox.rule;

import com.ctrip.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * 盲盒 ruleConfig 解析与活动窗口校验。
 */
public final class BlindBoxRuleConfigParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BlindBoxRuleConfigParser() {
    }

    public static ActivityWindow parse(String ruleConfig) {
        if (ruleConfig == null || ruleConfig.isBlank()) {
            throw new BusinessException("活动规则配置不能为空");
        }
        try {
            JsonNode root = MAPPER.readTree(ruleConfig);
            String startText = root.path("activityStartTime").asText(null);
            String endText = root.path("activityEndTime").asText(null);
            if (startText == null || endText == null) {
                throw new BusinessException("活动时间配置不完整");
            }
            LocalDateTime start = LocalDateTime.parse(startText);
            LocalDateTime end = LocalDateTime.parse(endText);
            if (!start.isBefore(end)) {
                throw new BusinessException("活动开始时间必须早于结束时间");
            }
            return new ActivityWindow(start, end);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("活动规则配置格式错误");
        }
    }

    public static void ensureActive(String ruleConfig, LocalDateTime now) {
        ActivityWindow window = parse(ruleConfig);
        if (now.isBefore(window.start())) {
            throw new BusinessException("活动未开始");
        }
        if (now.isAfter(window.end())) {
            throw new BusinessException("活动已结束");
        }
    }
}
