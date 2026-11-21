package com.ctrip.ai.prompt;

import com.ctrip.ai.dto.AiSchemeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI Prompt 构建测试")
class PromptBuilderTest {

    @Test
    @DisplayName("固定目的地：Prompt 明确不得更改")
    void destinationShouldBeFixed() {
        AiSchemeRequest request = new AiSchemeRequest(
                "上海", "STANDARD", "美食", 4, null, "杭州", 3L);

        String prompt = new PromptBuilder().build(request);

        assertThat(prompt).contains("目的地固定为杭州");
        assertThat(prompt).contains("不得更改");
        assertThat(prompt).doesNotContain("随机选择");
    }
}
