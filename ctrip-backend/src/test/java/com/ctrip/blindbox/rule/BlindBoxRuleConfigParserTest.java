package com.ctrip.blindbox.rule;

import com.ctrip.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("盲盒规则配置解析器单元测试")
class BlindBoxRuleConfigParserTest {

    private static final String VALID_RULE =
            "{\"activityStartTime\":\"2026-07-01T00:00:00\","
                    + "\"activityEndTime\":\"2026-07-07T23:59:59\","
                    + "\"description\":\"夏日限定\"}";

    @Test
    @DisplayName("合法配置：解析开始/结束时间")
    void parse_valid_shouldReturnWindow() {
        ActivityWindow window = BlindBoxRuleConfigParser.parse(VALID_RULE);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2026, 7, 7, 23, 59, 59));
    }

    @Test
    @DisplayName("缺少活动时间：抛出业务异常")
    void parse_missingFields_shouldThrow() {
        assertThatThrownBy(() -> BlindBoxRuleConfigParser.parse("{\"description\":\"x\"}"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("开始时间不早于结束时间：抛出业务异常")
    void parse_invalidOrder_shouldThrow() {
        String rule = "{\"activityStartTime\":\"2026-07-07T00:00:00\","
                + "\"activityEndTime\":\"2026-07-01T00:00:00\"}";

        assertThatThrownBy(() -> BlindBoxRuleConfigParser.parse(rule))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("非法 JSON / 非法时间：抛出业务异常")
    void parse_malformed_shouldThrow() {
        assertThatThrownBy(() -> BlindBoxRuleConfigParser.parse("not-json"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> BlindBoxRuleConfigParser.parse(
                "{\"activityStartTime\":\"bad\",\"activityEndTime\":\"2026-07-01T00:00:00\"}"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("活动窗口内可购买，窗口外拒绝")
    void ensureActive_shouldCheckWindow() {
        assertThatCode(() -> BlindBoxRuleConfigParser.ensureActive(
                VALID_RULE, LocalDateTime.of(2026, 7, 3, 12, 0)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> BlindBoxRuleConfigParser.ensureActive(
                VALID_RULE, LocalDateTime.of(2026, 6, 30, 12, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未开始");

        assertThatThrownBy(() -> BlindBoxRuleConfigParser.ensureActive(
                VALID_RULE, LocalDateTime.of(2026, 7, 8, 12, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结束");
    }
}
