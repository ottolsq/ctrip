# AI 模块设计

## 📋 一、整体架构

AI 模块是一个**独立的通用 AI 能力模块**，为整个项目提供统一的 AI 服务接口。其他业务模块（如盲盒、攻略、行程等）通过依赖注入调用 AI 模块，不直接与 DashScope SDK 耦合。

### 核心设计原则

- **模块解耦**：AI 模块位于 `com.ctrip.ai` 包下，与任何业务模块无关
- **接口抽象**：业务模块依赖 `AiSchemeService` 接口，不关心底层是哪个 AI 平台
- **可替换性**：未来可无缝切换 Claude/OpenAI/其他模型，业务模块零改动
- **Fallback 安全**：AI 失败时业务模块可自行回退到 mock 数据

### 包结构

```
src/main/java/com/ctrip/ai/
├── config/
│   └── DashScopeConfig.java              # DashScope 客户端配置
├── service/
│   ├── AiSchemeService.java              # AI 旅行方案生成接口
│   └── impl/
│       └── DashScopeAiSchemeService.java # DashScope 实现类
├── dto/
│   ├── AiSchemeRequest.java              # AI 方案请求 DTO
│   ├── AiSchemeResponse.java             # AI 原始响应 DTO（内部）
│   └── SchemeOutput.java                 # 标准化输出 DTO（给业务模块用）
└── prompt/
    ├── SystemPrompts.java                # System Prompt 模板
    └── PromptBuilder.java                # User Prompt 构建器
```

---

## 🤖 二、AI 服务接口

### AiSchemeService 接口

```java
package com.ctrip.ai.service;

/**
 * AI 旅行方案生成服务接口。
 *
 * <p>业务模块（盲盒、行程规划等）依赖此接口，不直接依赖 AI 平台 SDK。
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
```

### AiSchemeRequest DTO

```java
package com.ctrip.ai.dto;

/**
 * AI 旅行方案生成请求。
 *
 * <p>业务模块将自身参数转换为此 DTO，调用 AI 模块。
 */
public record AiSchemeRequest(
        String departureCity,    // 出发城市
        String budgetLevel,      // ECONOMY / STANDARD / LUXURY
        String theme,            // 美食/海滨/古镇/滑雪/文化/亲子...
        Integer preferredDays,   // 期望天数（可选，AI 可自行决定）
        String preferences       // 其他偏好 JSON（可选扩展）
) {
}
```

### SchemeOutput DTO（标准化输出）

```java
package com.ctrip.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI 生成的旅行方案标准化输出。
 *
 * <p>业务模块接收此 DTO，自行决定如何使用（序列化入库、返回前端等）。
 */
public record SchemeOutput(
        String destination,         // 目的地城市
        Long destinationId,         // 目的地ID（可选）
        String theme,               // 旅行主题
        Integer days,               // 行程天数
        Map<String, DayPlan> itinerary,  // 每日行程
        HotelInfo hotel,            // 酒店推荐
        TransportInfo transport,    // 交通方案
        BudgetInfo budget           // 预算明细
) {
    public record DayPlan(String title, List<String> activities) {}
    public record HotelInfo(String name, String address, Double rating) {}
    public record TransportInfo(String type, String departureTime, String description) {}
    public record BudgetInfo(Integer total, Integer transport, Integer hotel, Integer food, Integer tickets) {}
}
```

---

## 🔧 三、DashScope 实现

### DashScopeAiSchemeService

核心实现类，调用通义千问模型生成旅行方案。

**技术栈：**
- DashScope 官方 SDK：`com.alibaba:dashscope-sdk-java:2.22.15`
- Spring `@ConfigurationProperties` 管理配置
- `ObjectMapper` 解析 AI 返回的 JSON

**调用流程：**

```
AiSchemeRequest → PromptBuilder 构建 Prompt
               → DashScope SDK 调用 qwen-plus 模型
               → 清理 markdown 包裹
               → Jackson 解析 JSON → AiSchemeResponse
               → 转换为 SchemeOutput（标准化格式）
               → 返回给调用方
```

**关键代码结构：**

```java
@Service
public class DashScopeAiSchemeService implements AiSchemeService {

    private final Generation dashScopeGeneration;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;

    @Value("${app.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${app.ai.dashscope.model}")
    private String model;

    @Override
    public SchemeOutput generate(AiSchemeRequest request) {
        String userPrompt = promptBuilder.build(request);

        // 调用 DashScope
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(
                    new Message(Role.SYSTEM.getValue(), SystemPrompts.TRAVEL_PLANNER),
                    new Message(Role.USER.getValue(), userPrompt)))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(0.7)
                .maxTokens(4096)
                .build();

        GenerationResult result = dashScopeGeneration.call(param);
        String content = extractContent(result);
        content = stripMarkdownJson(content);

        AiSchemeResponse aiResponse = objectMapper.readValue(content, AiSchemeResponse.class);
        return convert(aiResponse, request);
    }

    /** 清理 AI 返回的 markdown 包裹。 */
    private String stripMarkdownJson(String content) {
        if (content.startsWith("```json")) {
            int end = content.indexOf("```", 7);
            if (end > 0) return content.substring(7, end).trim();
        } else if (content.startsWith("```")) {
            int end = content.indexOf("```", 3);
            if (end > 0) return content.substring(3, end).trim();
        }
        return content;
    }

    /** 将 AiSchemeResponse 转换为标准化的 SchemeOutput。 */
    private SchemeOutput convert(AiSchemeResponse ai, AiSchemeRequest request) {
        return new SchemeOutput(
                ai.destination(),
                resolveDestinationId(ai.destination()),
                ai.theme(),
                ai.days(),
                ai.itinerary() != null
                    ? ai.itinerary().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> new DayPlan(e.getValue().title(), e.getValue().activities())))
                    : Map.of(),
                new HotelInfo(ai.hotel().name(), ai.hotel().address(), ai.hotel().rating()),
                new TransportInfo(ai.transport().type(), ai.transport().departureTime(), ai.transport().description()),
                new BudgetInfo(ai.budget().total(), ai.budget().transport(), ai.budget().hotel(),
                    ai.budget().food(), ai.budget().tickets())
        );
    }
}
```

### DashScopeConfig

```java
@Configuration
@ConfigurationProperties(prefix = "app.ai.dashscope")
public class DashScopeConfig {
    private String apiKey;
    private String model = "qwen-plus";
    private int timeoutMs = 25000;
    // getter/setter...

    @Bean
    public Generation dashScopeGeneration() {
        return new Generation();
    }
}
```

---

## 📝 四、Prompt 设计

### SystemPrompts.java

```java
public final class SystemPrompts {
    private SystemPrompts() {}

    /** 旅行规划师 System Prompt。 */
    public static final String TRAVEL_PLANNER = """
        你是一个专业的旅行规划师。你的任务是根据用户的基本信息，
        生成一份详细、可行、个性化的旅行方案。

        要求：
        1. 目的地必须是用户指定的城市和区域，行程真实可行
        2. 行程安排要符合该预算等级的实际消费水平
        3. 酒店推荐要符合预算等级（经济型=快捷/舒适，标准型=精品/四星，豪华型=五星/度假）
        4. 交通方式要合理（从出发城市到目的地）
        5. 预算明细要合理分配，transport + hotel + food + tickets = total
        6. 所有金额单位为人民币（CNY）
        7. 每个 day 的 activities 必须包含具体的、可行的、有特色的活动

        你必须严格按照以下 JSON 格式返回，不要输出任何其他文字。

        输出 JSON 结构：
        {
          "destination": "目的地城市名称",
          "theme": "旅行主题",
          "days": 行程天数,
          "itinerary": {
            "day1": { "title": "抵达XX", "activities": ["活动1", "活动2"] },
            "day2": { "title": "主题体验日", "activities": ["上午活动", "午餐推荐", "下午活动", "晚餐"] }
          },
          "hotel": { "name": "酒店名称", "address": "酒店地址区域", "rating": 4.2 },
          "transport": { "type": "飞机/高铁", "departureTime": "建议出发时间", "description": "交通方案详情" },
          "budget": { "total": 2000, "transport": 600, "hotel": 800, "food": 400, "tickets": 200 }
        }
        """;
}
```

### PromptBuilder.java

根据 `AiSchemeRequest` 动态构建 User Prompt：

```java
@Component
public class PromptBuilder {

    public String build(AiSchemeRequest request) {
        String budgetDesc = switch (request.budgetLevel()) {
            case "ECONOMY" -> "经济型（总预算约 1500-2000 元）";
            case "STANDARD" -> "标准型（总预算约 2500-3500 元）";
            case "LUXURY" -> "豪华型（总预算约 5000-8000 元）";
            default -> "标准型";
        };

        String themeHint = (request.theme() != null && !request.theme().isBlank())
                ? "用户偏好的主题是「" + request.theme() + "」。请在行程中充分体现这一主题。"
                : "用户没有指定主题，请随机选择一个适合该目的地的主题。";

        String daysHint = (request.preferredDays() != null)
                ? "用户期望行程天数为 " + request.preferredDays() + " 天。"
                : "请根据预算等级自动决定天数（ECONOMY=3天，STANDARD=4天，LUXURY=5天）。";

        return String.format("""
            请为以下用户生成旅行方案：

            - 出发城市：%s
            - 预算等级：%s - %s
            - 主题偏好：%s
            - 行程天数：%s

            请从 %s 出发，规划到目的地的合理交通方案。
            请严格按照 JSON 格式返回完整方案。
            """,
            request.departureCity(),
            request.budgetLevel(),
            budgetDesc,
            themeHint,
            daysHint,
            request.departureCity());
    }
}
```

---

## ⚠️ 五、异常设计

### AiServiceException

```java
package com.ctrip.ai.exception;

/** AI 服务通用异常，调用方需自行处理。 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message, Throwable cause) { super(message, cause); }
}

/** AI 可回退异常，业务模块可捕获后启用 fallback。 */
public class AiFallbackException extends AiServiceException {
    public AiFallbackException(String message, Throwable cause) { super(message, cause); }
}
```

**使用方式：**
- 业务模块调用 `AiSchemeService.generate()`
- 正常情况返回 `SchemeOutput`
- 异常时抛出 `AiServiceException`，业务模块自行决定 fallback 策略

---

## 🗄️ 六、数据模型

### 无独立数据表

AI 模块不直接操作数据库，仅作为**无状态的服务接口**。

- **输入**：`AiSchemeRequest`（业务模块构造）
- **输出**：`SchemeOutput`（业务模块使用）
- **存储**：由调用方决定如何持久化（如盲盒模块存入 `blind_box_result.resultText`）

---

## ⚙️ 七、配置

### application.properties

```properties
# ===== AI 模块 (DashScope / 通义千问) =====
# API key 获取：https://bailian.console.aliyun.com/
# 生产环境通过环境变量注入
app.ai.dashscope.api-key=${AI_DASHSCOPE_API_KEY:sk-test-placeholder}
app.ai.dashscope.model=qwen-plus
app.ai.dashscope.timeout-ms=25000
# AI 调用失败时是否允许回退到 mock（由业务模块自行实现）
app.ai.fallback-enabled=true
```

---

## 🔄 八、与盲盒模块的集成方式

### 盲盒模块如何调用

```java
// BlindBoxSchemeServiceImpl.java
@Service
public class BlindBoxSchemeServiceImpl implements BlindBoxSchemeService {

    private final AiSchemeService aiSchemeService;

    @Override
    public SchemeResponse generateScheme(BlindBoxPreference preference) {
        AiSchemeRequest request = new AiSchemeRequest(
                preference.getDepartureCity(),
                preference.getBudgetLevel().name(),
                preference.getTheme(),
                null, null
        );

        try {
            SchemeOutput output = aiSchemeService.generate(request);
            return convertToSchemeResponse(output);
        } catch (AiFallbackException e) {
            log.warn("AI 失败，回退到 mock");
            return generateMockScheme(preference);
        }
    }
}
```

### 依赖关系

```
pom.xml（ctrip 根项目）
  └── dashscope-sdk-java 依赖（只声明一次）

com.ctrip.ai.*            # AI 模块（通用）
  └── AiSchemeService     # 接口
  └── DashScopeAiSchemeService  # DashScope 实现

com.ctrip.blindbox.*      # 盲盒模块（业务模块）
  └── BlindBoxSchemeServiceImpl  # 注入 AiSchemeService，调用 AI
```

**关键**：盲盒模块不直接依赖 DashScope SDK，只依赖 `AiSchemeService` 接口。

---

## 📦 九、Maven 依赖

```xml
<!-- DashScope AI SDK：通义千问 AI 集成 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>2.22.15</version>
</dependency>
```

---

## 🔍 十、测试方案

### 单元测试

- `DashScopeAiSchemeServiceTest`：Mock DashScope SDK，验证 prompt 构建、JSON 解析、DTO 转换逻辑
- `PromptBuilderTest`：验证不同偏好组合生成的 prompt 正确

### 集成测试

- 配置测试用 API key，验证真实调用 DashScope 并返回有效 JSON
- 测试 fallback 路径（设置无效 API key）

### 验证方式

1. **编译**：`mvn compile`
2. **AI 正常**：有效 API key → 开盒 → 检查行程真实感
3. **Fallback**：无效 API key → 开盒 → mock 数据正常返回
4. **日志**：
   - 成功：`INFO AI 方案生成成功: destination=成都, days=3`
   - 失败：`WARN AI 调用失败: error=xxx`

---

## 📊 十一、API 端点

AI 模块**不暴露独立的 HTTP 端点**，仅作为内部服务接口。

业务模块通过 Spring 依赖注入调用：
```java
@Autowired
private AiSchemeService aiSchemeService;
```

---

## ✅ 十二、总结

### 模块质量

| 维度 | 评级 | 说明 |
|------|------|------|
| 解耦性 ⭐⭐⭐⭐⭐ | ✅ 完全独立，业务模块零耦合 |
| 可替换性 ⭐⭐⭐⭐⭐ | ✅ 接口抽象，换模型只需改实现 |
| 可维护性 ⭐⭐⭐⭐⭐ | ✅ 职责清晰，包结构合理 |
| 容错性 ⭐⭐⭐⭐⭐ | ✅ 异常 + Fallback 双重保护 |
| 生产就绪度 ⭐⭐⭐⭐ | ⚠️ 待补充限流、缓存、重试等 |

### 待补充（后续迭代）

- [ ] AI 调用限流（防止并发过高消耗 quota）
- [ ] 结果缓存（相同参数短时间不重复调用）
- [ ] 自动重试（Resilience4j / Spring Retry）
- [ ] 多模型支持（Claude、OpenAI 实现类）
- [ ] 流式输出支持（SSE）
- [ ] 调用日志 & 监控指标
