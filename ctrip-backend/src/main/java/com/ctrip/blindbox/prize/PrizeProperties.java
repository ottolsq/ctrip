package com.ctrip.blindbox.prize;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    void validate() {
        if (guaranteeCount <= 0) {
            throw new IllegalStateException("盲盒保底次数必须大于 0");
        }
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalStateException("盲盒概率档位不能为空");
        }
        for (TierConfig tier : tiers) {
            if (tier.getTier() == null || tier.getWeight() <= 0) {
                throw new IllegalStateException("盲盒档位与权重配置非法");
            }
        }
    }

    @Data
    public static class TierConfig {

        private PrizeTier tier;

        private int weight;

        private List<Long> destinationIds = new ArrayList<>();
    }
}
