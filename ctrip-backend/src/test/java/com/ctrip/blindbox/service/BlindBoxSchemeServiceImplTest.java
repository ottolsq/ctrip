package com.ctrip.blindbox.service;

import com.ctrip.ai.dto.AiSchemeRequest;
import com.ctrip.ai.dto.SchemeOutput;
import com.ctrip.ai.service.AiSchemeService;
import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.blindbox.entity.BlindBoxPreference;
import com.ctrip.blindbox.entity.enums.BudgetLevel;
import com.ctrip.blindbox.prize.DrawnPrize;
import com.ctrip.blindbox.service.impl.BlindBoxSchemeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("盲盒方案生成：固定抽中目的地")
class BlindBoxSchemeServiceImplTest {

    @Mock
    private AiSchemeService aiSchemeService;

    private BlindBoxPreference preference() {
        return BlindBoxPreference.builder()
                .id(1L)
                .departureCity("上海")
                .budgetLevel(BudgetLevel.STANDARD)
                .theme("美食")
                .build();
    }

    private DrawnPrize prize() {
        return new DrawnPrize("RARE", 3L, "杭州", false, "v1");
    }

    private BlindBoxSchemeServiceImpl service() {
        return new BlindBoxSchemeServiceImpl(aiSchemeService, true);
    }

    @Test
    @DisplayName("AI 请求携带抽中目的地，结果不被 AI 改写")
    void aiRequestShouldCarryPrizeDestination() {
        when(aiSchemeService.generate(any())).thenReturn(new SchemeOutput(
                "北京", 2L, "文化", 4,
                Map.of(),
                null, null, null));

        SchemeResponse response = service().generateScheme(preference(), prize());

        ArgumentCaptor<AiSchemeRequest> captor = ArgumentCaptor.forClass(AiSchemeRequest.class);
        verify(aiSchemeService).generate(captor.capture());
        assertThat(captor.getValue().destination()).isEqualTo("杭州");
        assertThat(captor.getValue().destinationId()).isEqualTo(3L);
        assertThat(response.destination()).isEqualTo("杭州");
        assertThat(response.destinationId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("AI 失败回退：mock 方案仍使用抽中目的地")
    void mockFallbackShouldUsePrizeDestination() {
        when(aiSchemeService.generate(any())).thenThrow(new RuntimeException("ai down"));

        SchemeResponse response = service().generateScheme(preference(), prize());

        assertThat(response.destination()).isEqualTo("杭州");
        assertThat(response.destinationId()).isEqualTo(3L);
    }
}
