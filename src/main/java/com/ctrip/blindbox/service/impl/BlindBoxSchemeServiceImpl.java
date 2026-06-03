package com.ctrip.blindbox.service.impl;

import com.ctrip.blindbox.dto.SchemeResponse;
import com.ctrip.blindbox.entity.BlindBoxPreference;
import com.ctrip.blindbox.service.BlindBoxSchemeService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 盲盒方案生成服务实现（MVP mock）。
 *
 * <p>MVP 阶段基于规则生成 mock 旅行方案，后续接入 AI API 替换此实现。
 * 根据预算等级和主题选择不同的目的地和价格区间。
 */
@Service
public class BlindBoxSchemeServiceImpl implements BlindBoxSchemeService {

    private static final Random RANDOM = new Random();

    /** 预设目的地池 */
    private static final String[][] DAILY_DESTINATIONS = {
            {"成都", "美食", "6"},
            {"杭州", "古镇", "3"},
            {"三亚", "海滨", "7"},
            {"西安", "文化", "4"},
            {"丽江", "古镇", "8"},
            {"哈尔滨", "滑雪", "10"},
            {"广州", "美食", "1"},
            {"厦门", "海滨", "5"},
            {"桂林", "山水", "9"},
            {"北京", "文化", "2"},
    };

    @Override
    public SchemeResponse generateScheme(BlindBoxPreference preference) {
        // 根据主题或随机选择目的地
        String[] dest = selectDestination(preference.getTheme());

        String destination = dest[0];
        String theme = dest[1];
        Long destinationId = Long.valueOf(dest[2]);
        String budgetLevel = preference.getBudgetLevel().name();

        // 根据预算等级确定天数和价格区间
        int days = switch (preference.getBudgetLevel()) {
            case ECONOMY -> 3;
            case STANDARD -> 4;
            case LUXURY -> 5;
        };

        int budgetTotal = switch (preference.getBudgetLevel()) {
            case ECONOMY -> 1500 + RANDOM.nextInt(500);
            case STANDARD -> 2500 + RANDOM.nextInt(1000);
            case LUXURY -> 5000 + RANDOM.nextInt(3000);
        };

        // 生成行程 mock 数据
        Map<String, Object> itinerary = buildMockItinerary(days, destination, theme);
        Map<String, Object> hotel = buildMockHotel(destination, budgetLevel);
        Map<String, Object> transport = buildMockTransport(destination);
        Map<String, Object> budget = buildMockBudget(budgetTotal);

        return new SchemeResponse(
                destination,
                destinationId,
                theme,
                budgetLevel,
                days,
                itinerary,
                hotel,
                transport,
                budget
        );
    }

    /** 根据主题筛选或随机选择目的地。 */
    private String[] selectDestination(String theme) {
        if (theme != null && !theme.isBlank()) {
            for (String[] dest : DAILY_DESTINATIONS) {
                if (dest[1].equals(theme)) {
                    return dest;
                }
            }
        }
        return DAILY_DESTINATIONS[RANDOM.nextInt(DAILY_DESTINATIONS.length)];
    }

    /** 生成 mock 行程安排。 */
    private Map<String, Object> buildMockItinerary(int days, String destination, String theme) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] themes = switch (theme) {
            case "美食" -> new String[]{"品尝地道小吃", "逛美食街", "学做地方菜", "品尝老字号"};
            case "海滨" -> new String[]{"海滩漫步", "潜水体验", "海鲜大餐", "日落观赏"};
            case "古镇" -> new String[]{"古城漫步", "民俗体验", "手工艺制作", "夜景游览"};
            case "滑雪" -> new String[]{"滑雪教学", "自由滑行", "雪地温泉", "冰雕展览"};
            case "文化" -> new String[]{"博物馆参观", "古迹游览", "非遗体验", "剧场演出"};
            default -> new String[]{"市区观光", "特色体验", "自由活动", "返程"};
        };

        for (int i = 1; i <= days; i++) {
            Map<String, Object> day = new LinkedHashMap<>();
            String title = (i == 1) ? "抵达" + destination : (i == days) ? "返程" : themes[(i - 2) % themes.length];
            day.put("title", title);
            day.put("items", List.of());
            result.put("day" + i, day);
        }
        return result;
    }

    /** 生成 mock 酒店信息。 */
    private Map<String, Object> buildMockHotel(String destination, String budgetLevel) {
        Map<String, Object> hotel = new LinkedHashMap<>();
        String hotelName = switch (budgetLevel) {
            case "ECONOMY" -> destination + "舒适酒店";
            case "STANDARD" -> destination + "精品酒店";
            case "LUXURY" -> destination + "豪华度假酒店";
            default -> destination + "酒店";
        };
        hotel.put("name", hotelName);
        hotel.put("address", destination + "市中心");
        hotel.put("rating", switch (budgetLevel) {
            case "ECONOMY" -> 3.5;
            case "STANDARD" -> 4.2;
            case "LUXURY" -> 4.8;
            default -> 4.0;
        });
        return hotel;
    }

    /** 生成 mock 交通信息。 */
    private Map<String, Object> buildMockTransport(String destination) {
        Map<String, Object> transport = new LinkedHashMap<>();
        transport.put("type", RANDOM.nextBoolean() ? "高铁" : "飞机");
        transport.put("departureTime", "出发日期当天 09:00");
        transport.put("destination", destination);
        return transport;
    }

    /** 生成 mock 预算明细。 */
    private Map<String, Object> buildMockBudget(int total) {
        Map<String, Object> budget = new LinkedHashMap<>();
        int transport = (int) (total * 0.25);
        int hotel = (int) (total * 0.40);
        int food = (int) (total * 0.20);
        int tickets = total - transport - hotel - food;
        budget.put("total", total);
        budget.put("transport", transport);
        budget.put("hotel", hotel);
        budget.put("food", food);
        budget.put("tickets", tickets);
        return budget;
    }
}
