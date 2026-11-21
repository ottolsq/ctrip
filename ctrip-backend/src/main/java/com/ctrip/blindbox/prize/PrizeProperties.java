package com.ctrip.blindbox.prize;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 盲盒概率与保底配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.blindbox.prize")
public class PrizeProperties {

    private int guaranteeCount = 10;

    private String probabilityVersion = "v1";

    private List<TierConfig> tiers = new ArrayList<>();

    @Data
    public static class TierConfig {

        private PrizeTier tier;

        private int weight;

        private List<Long> destinationIds = new ArrayList<>();
    }
}
