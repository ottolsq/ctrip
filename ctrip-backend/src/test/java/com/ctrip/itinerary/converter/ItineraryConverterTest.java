package com.ctrip.itinerary.converter;

import com.ctrip.itinerary.dto.response.ItineraryResponse;
import com.ctrip.itinerary.entity.Itinerary;
import com.ctrip.itinerary.entity.enums.ItineraryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ItineraryConverter} 单元测试。
 */
@DisplayName("行程转换器单元测试")
class ItineraryConverterTest {

    private Itinerary sampleItinerary() {
        return Itinerary.builder()
                .id(1L)
                .userId(7L)
                .title("上海周末")
                .status(ItineraryStatus.PUBLISHED)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 3))
                .build();
    }

    @Test
    @DisplayName("本人查看：isOwner 为 true，状态映射正确")
    void toResponse_shouldSetOwnerFlagWhenViewerIsOwner() {
        ItineraryResponse response = ItineraryConverter.toResponse(
                sampleItinerary(), List.of(), List.of(), true);

        assertThat(response.isOwner()).isTrue();
        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("匿名分享：isOwner 为 false")
    void toResponse_shouldSetOwnerFalseForAnonymousViewer() {
        ItineraryResponse response = ItineraryConverter.toResponse(
                sampleItinerary(), List.of(), List.of(), false);

        assertThat(response.isOwner()).isFalse();
    }

    @Test
    @DisplayName("JSON 序列化字段名为 isOwner")
    void toResponse_shouldSerializeIsOwnerFieldName() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        String json = mapper.writeValueAsString(ItineraryConverter.toResponse(
                sampleItinerary(), List.of(), List.of(), true));

        assertThat(json).contains("\"isOwner\":true");
    }
}
