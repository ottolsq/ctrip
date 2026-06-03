# 盲盒模块逻辑分析

> 基于 `docs/ctrip_modules.md` 和 `docs/ctrip_plan.md` 中 Phase 4 的设计方案，参考 `docs/user_module.md` 的文档结构编写。
> 所有 API 接口遵循 `docs/other/api_standards.md` 规范，代码实现参考 `docs/other/redis_rabbitmq_design.md`。

---

## 📋 一、整体架构

盲盒模块采用经典的**三层架构**：

- **Controller层**：处理HTTP请求和响应（`/api/v1/blind-box/**`）
- **Service层**：业务逻辑处理（盲盒模板、订单、结果、偏好、库存）
- **Mapper层**：数据访问（MyBatis Plus）

### 1.1 核心组件

| 组件 | 路径 | 职责 |
|------|------|------|
| BlindBoxController | `/api/v1/blind-box/**` | 盲盒列表/详情/开盒入口 |
| BlindBoxOrderController | `/api/v1/blind-box/orders/**` | 订单创建、查询、取消、开盒 |
| AdminBlindBoxController | `/api/v1/admin/blind-box/**` | 后台模板管理 |
| BlindBoxTemplateService | `com.ctrip.blindbox.service` | 模板管理与库存操作 |
| BlindBoxOrderService | `com.ctrip.blindbox.service` | 订单创建、支付、超时取消 |
| BlindBoxResultService | `com.ctrip.blindbox.service` | 结果生成、查询、分享 |
| BlindBoxSchemeService | `com.ctrip.blindbox.service` | 盲盒方案生成（AI 预留接口） |

### 1.2 盲盒类型定义

| 类型 | 枚举值 | 说明 | 库存策略 |
|------|--------|------|---------|
| **日常盲盒** | `DAILY` | 不限量，用户随时购买，价格稳定 | `stock = -1`（无限） |
| **限定盲盒** | `LIMITED` | 限时限量抢购，可设置特殊主题/折扣 | `stock > 0`，需扣减 |

### 1.3 暂不实现功能（后续迭代）

> 以下功能在 MVP 阶段暂不实现，保留设计文档，后续接入：
>
> - **AI 图片分析**：`POST /api/v1/blind-box/analyze-image`（AI 处理图片提取特征信息的接口尚未实现）
> - **AI 结果图生成**：盲盒结果图中的 AI 可视化行程单图片（`result_image_url` 字段暂不使用）
> - **结果图下载**：`GET /api/v1/blind-box/orders/{orderNo}/result-image`
> - `blind_box_preference.image_tags` 字段：AI 图片分析结果存储

---

## 🗄️ 二、数据模型设计

> SQL 文件待创建于 `docs/SQL/ctrip_blindbox.sql`。

### 2.1 BlindBoxTemplate 实体（blind_box_template 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 盲盒名称 |
| type | BlindBoxType | 类型枚举（DAILY/LIMITED） |
| price | BigDecimal | 价格（单位：元） |
| stock | Integer | 库存（-1=无限，>=0=限量） |
| ruleConfig | String(JSON) | 规则配置（含活动起止时间、折扣等） |
| status | TemplateStatus | 状态枚举（ACTIVE/INACTIVE） |
| createdAt | LocalDateTime | 创建时间（自动填充） |
| updatedAt | LocalDateTime | 更新时间（自动填充） |

**盲盒类型枚举（BlindBoxType）：**

| 类型 | 值 | 说明 |
|------|-----|------|
| DAILY | 0 | 日常盲盒，不限量，随时购买 |
| LIMITED | 1 | 限定盲盒，限时限量抢购 |

**模板状态枚举（TemplateStatus）：**

| 状态 | 值 | 说明 |
|------|-----|------|
| ACTIVE | 0 | 上架中，用户可购买 |
| INACTIVE | 1 | 已下架 |

**ruleConfig JSON 结构：**
```json
{
  "activityStartTime": "2026-07-01T00:00:00",
  "activityEndTime": "2026-07-07T23:59:59",
  "discount": 0.8,
  "description": "夏日限定海岛盲盒"
}
```

**设计要点：**
- `type = DAILY` 时 `stock` 必须为 `-1`，不检查库存
- `type = LIMITED` 时 `stock > 0`，购买时需扣减库存，`stock = 0` 表示已售罄
- 限定盲盒的活动时间通过 `ruleConfig` 配置，用于前端展示"限时抢购"标签
- 库存扣减参考 `docs/other/redis_rabbitmq_design.md` 第九章节方案

---

### 2.2 BlindBoxOrder 实体（blind_box_order 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| userId | Long | 关联的用户ID（FK → users） |
| templateId | Long | 盲盒模板ID（FK → blind_box_template） |
| orderNo | String | 订单编号（业务主键，格式：BB + 日期 + 序号） |
| status | OrderStatus | 订单状态（见下方） |
| payAmount | BigDecimal | 实际支付金额 |
| payMethod | String | 支付方式（"ALIPAY" / "WECHAT_PAY"，MVP阶段可null） |
| payTime | LocalDateTime | 支付时间 |
| expireAt | LocalDateTime | 支付超时时间（创建后15分钟） |
| createdAt | LocalDateTime | 创建时间（自动填充） |
| updatedAt | LocalDateTime | 更新时间（自动填充） |

**订单状态枚举（OrderStatus）：**

| 状态 | 值 | 说明 |
|------|-----|------|
| PENDING | 0 | 待支付（创建后15分钟超时） |
| PAID | 1 | 已支付，待开盒 |
| OPENED | 2 | 已开盒 |
| REFUNDED | 3 | 已退款 |
| CANCELLED | 4 | 已取消（超时未支付/用户主动取消） |

**设计要点：**
- `orderNo` 为业务主键，格式 `BByyyyMMddXXXX`（如 `BB202606020001`），对外暴露使用此编号
- `expireAt` 为创建时间 + 15 分钟，定时任务扫描过期订单自动取消
- 订单超时取消后，若为限定盲盒需恢复库存

---

### 2.3 BlindBoxResult 实体（blind_box_result 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| orderId | Long | 关联的订单ID（FK → blind_box_order） |
| destination | String | 目的地名称 |
| destinationId | Long | 目的地ID（FK → destination） |
| theme | String | 旅行主题（美食/海滨/古镇/滑雪/亲子等） |
| resultText | String(LONGTEXT JSON) | 完整旅行方案 JSON（目的地/行程/酒店/交通/预算） |
| ~~resultImageUrl~~ | ~~String~~ | **暂不实现**：AI 生成的可视化行程单图片 URL |
| shareCode | String(6) | 分享链接短码（6位 Base62） |
| shareExpiresAt | LocalDateTime | 分享链接过期时间 |
| openedAt | LocalDateTime | 开盒时间 |
| createdAt | LocalDateTime | 创建时间（自动填充） |

**resultText JSON 结构：**
```json
{
  "destination": "成都",
  "destinationId": 5,
  "theme": "美食",
  "budgetLevel": "STANDARD",
  "days": 4,
  "itinerary": {
    "day1": { "title": "抵达成都", "items": [] },
    "day2": { "title": "宽窄巷子-锦里", "items": [] },
    "day3": { "title": "都江堰-青城山", "items": [] },
    "day4": { "title": "返程", "items": [] }
  },
  "hotel": { "name": "成都某某酒店", "address": "...", "rating": 4.5 },
  "transport": { "type": "高铁", "departureTime": "..." },
  "budget": { "total": 2999, "transport": 800, "hotel": 1200, "food": 600, "tickets": 399 }
}
```

**设计要点：**
- 开盒时一次性生成 `resultText`，不可更改
- `result_image_url`（**暂不实现**：AI 结果图生成功能尚未就绪）
- `shareCode` 为 6 位 Base62 短码，用于分享链接 `GET /api/v1/blind-box/share/{shareCode}`

---

### 2.4 BlindBoxPreference 实体（blind_box_preference 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| orderId | Long | 关联的订单ID（FK → blind_box_order） |
| departureCity | String | 出发城市 |
| budgetLevel | BudgetLevel | 预算等级（ECONOMY/STANDARD/LUXURY） |
| theme | String | 旅行主题（可选） |
| ~~imageTags~~ | ~~String(JSON)~~ | **暂不实现**：AI 分析图片返回的特征标签 |
| createdAt | LocalDateTime | 创建时间（自动填充） |

**预算等级枚举（BudgetLevel）：**

| 等级 | 值 | 说明 |
|------|-----|------|
| ECONOMY | 0 | 经济型（低价位酒店/交通） |
| STANDARD | 1 | 标准型（中等价位） |
| LUXURY | 2 | 豪华型（高端酒店/交通） |

**设计要点：**
- 预选参数在创建订单时一并提交，与订单在同一事务中保存
- `image_tags` 字段（**暂不实现**：AI 图片分析接口尚未就绪）

---

### 2.5 表关系图

```
users (1) ──< blind_box_order (N) >── (1) blind_box_template
                                          │
                                          │ (1对1)
                                          ▼
                                   blind_box_result
                                          ▲
                                          │
blind_box_order (1) ──< blind_box_preference (1)
```

---

## 🔐 三、盲盒模板模块（Template）

### 1. 盲盒列表 (`GET /api/v1/blind-box`)

**流程：**

1. 查询所有 `status = ACTIVE` 的模板
2. 根据模板类型区分展示：
   - 日常盲盒：显示价格、"随时可买" 标签
   - 限定盲盒：显示价格、库存数量、活动倒计时标签
3. 按创建时间倒序排列

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| type | String | 否 | 筛选：DAILY / LIMITED |

**响应示例：**
```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "日常旅行盲盒",
        "type": "DAILY",
        "price": 99.00,
        "stock": -1,
        "ruleConfig": { "description": "日常随机旅行体验" },
        "status": "ACTIVE",
        "createdAt": "2026-06-01T10:00:00"
      },
      {
        "id": 2,
        "name": "夏日海岛限定盲盒",
        "type": "LIMITED",
        "price": 199.00,
        "stock": 50,
        "ruleConfig": { "activityStartTime": "2026-07-01T00:00:00", "activityEndTime": "2026-07-07T23:59:59", "description": "夏日限定海岛盲盒" },
        "status": "ACTIVE",
        "createdAt": "2026-06-01T10:00:00"
      }
    ],
    "total": 2,
    "page": 1,
    "limit": 20,
    "pages": 1
  },
  "error": null
}
```

**关键代码位置：**
- Controller: `BlindBoxController.listTemplates()`
- Service: `BlindBoxTemplateService.listActiveTemplates()`

---

### 2. 模板详情 (`GET /api/v1/blind-box/{id}`)

**流程：**

1. 根据 ID 查询模板
2. 验证模板状态为 ACTIVE
3. 返回详情（含规则说明）

**关键代码位置：**
- Controller: `BlindBoxController.getTemplate()`
- Service: `BlindBoxTemplateService.getTemplateDetail()`

---

## 🛒 四、购买与支付模块（Order）

### 1. 创建订单 (`POST /api/v1/blind-box/orders`)

**认证要求：** 需要 JWT

**流程：**

1. 从 SecurityContext 获取当前 userId
2. 验证模板是否存在且状态为 ACTIVE
3. 验证模板类型：
   - `DAILY`：无需检查库存
   - `LIMITED`：检查库存 `stock > 0`，检查活动时间是否在有效期内
4. 创建订单（状态 = PENDING，expireAt = now + 15分钟）
5. 保存预选参数到 `blind_box_preference`
6. 若为限定盲盒，扣减库存（使用分布式锁 + 乐观锁，详见下方「库存扣减」）
7. 返回订单信息

**请求参数：**
```json
{
  "templateId": 1,
  "departureCity": "上海",
  "budgetLevel": "STANDARD",
  "theme": "美食"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateId | Long | 是 | 盲盒模板ID |
| departureCity | String | 是 | 出发城市 |
| budgetLevel | String | 是 | 预算等级：ECONOMY / STANDARD / LUXURY |
| theme | String | 否 | 旅行主题（可选） |

**响应（HTTP 201）：**
```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "templateId": 1,
    "templateName": "日常旅行盲盒",
    "payAmount": 99.00,
    "expireAt": "2026-06-02T10:45:00",
    "status": "PENDING"
  },
  "error": null
}
```

**关键代码位置：** `BlindBoxOrderService.createOrder()`

**事务保护：** 订单创建 + 偏好保存 + 库存扣减在同一事务内（限定盲盒需分布式锁）

---

### 2. 订单查询 (`GET /api/v1/blind-box/orders/{orderNo}`)

**认证要求：** 需要 JWT

**流程：**

1. 根据 orderNo 查询订单
2. 验证订单属于当前用户
3. 返回订单详情

---

### 3. 我的订单列表 (`GET /api/v1/blind-box/orders`)

**认证要求：** 需要 JWT

**流程：**

1. 从 SecurityContext 获取当前 userId
2. 查询该用户的所有盲盒订单（分页）
3. 按创建时间倒序排列

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| status | String | 否 | 筛选订单状态 |

---

### 4. 取消订单 (`POST /api/v1/blind-box/orders/{orderNo}/cancel`)

**认证要求：** 需要 JWT

**流程：**

1. 验证订单属于当前用户
2. 验证订单状态为 PENDING（仅待支付可取消）
3. 更新订单状态为 CANCELLED
4. 若为限定盲盒，恢复库存（stock + 1）
5. 记录日志

---

### 5. 模拟支付回调 (`POST /api/v1/payments/blind-box/callback`)

**说明：** MVP 阶段使用模拟支付，不对接真实支付渠道。

**流程：**

1. 接收回调参数（orderNo、payMethod 等）
2. 幂等检查：若订单已支付，直接返回成功
3. 验证订单存在且状态为 PENDING
4. 更新订单状态为 PAID，记录 payTime 和 payMethod

**请求参数：**
```json
{
  "orderNo": "BB202606020001",
  "payMethod": "ALIPAY"
}
```

---

## 🎁 五、开盒模块（Open Box）

### 1. 开盒 (`POST /api/v1/blind-box/orders/{orderNo}/open`)

**认证要求：** 需要 JWT

**流程：**

1. 从 SecurityContext 获取当前 userId
2. 验证订单属于当前用户
3. 验证订单状态为 PAID（仅已支付可开盒）
4. 获取分布式锁 `lock:blindbox:open:{orderId}` 防止重复开盒
5. 检查开盒幂等性：若已开盒，直接返回已有结果
6. 更新订单状态为 OPENED
7. 调用盲盒方案生成服务：
   - 读取 `blind_box_preference` 中的预选参数
   - 根据预算、主题从目的地数据库按权重随机选取
   - 生成完整旅行方案 JSON
8. 生成分享短码（6 位 Base62，防冲突检查）
9. 保存到 `blind_box_result` 表
10. 记录开盒时间 `openedAt`
11. 释放锁，返回结果

**关键代码位置：** `BlindBoxOrderService.openBox()`

**并发安全：**
- 使用 Redis 分布式锁 `lock:blindbox:open:{orderId}` 防止重复开盒
- 开盒幂等：若已开盒，直接返回已有结果

---

## 📦 六、盲盒结果模块（Result）

### 1. 我的盲盒列表 (`GET /api/v1/blind-box/my`)

**认证要求：** 需要 JWT

**流程：**

1. 从 SecurityContext 获取当前 userId
2. 查询该用户的所有订单及对应的盲盒结果（分页）
3. 按开盒时间倒序排列

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| opened | Boolean | 否 | 筛选：true=已开盒，false=未开盒 |

---

### 2. 结果详情 (`GET /api/v1/blind-box/orders/{orderNo}/result`)

**认证要求：** 需要 JWT

**流程：**

1. 验证订单属于当前用户
2. 验证订单状态为 OPENED（已开盒）
3. 返回盲盒结果（含完整旅行方案）

---

### 3. ~~下载结果图~~（暂不实现）

> **暂不实现**：AI 生成结果图功能尚未就绪。
> `GET /api/v1/blind-box/orders/{orderNo}/result-image`

---

## 🔗 七、分享模块（Share）

### 1. 生成分享链接 (`POST /api/v1/blind-box/orders/{orderNo}/share-result`)

**认证要求：** 需要 JWT

**流程：**

1. 验证订单属于当前用户
2. 验证订单已开盒
3. 获取已有的 `shareCode`（若已分享过则复用）
4. 设置分享过期时间（默认 7 天）
5. 返回分享链接

---

### 2. 查看分享结果 (`GET /api/v1/blind-box/share/{shareCode}`)

**认证要求：** 无需认证（公开接口）

**流程：**

1. 根据 shareCode 查询盲盒结果
2. 验证分享链接未过期
3. 返回旅行方案概览（**结果图暂不展示**）

---

## 🔧 八、库存扣减策略

> 参考 `docs/other/redis_rabbitmq_design.md` 第九章节「秒杀 / 高并发库存扣减方案」。

### 8.1 日常盲盒

- `stock = -1`，不检查库存，不扣减
- 直接创建订单

### 8.2 限定盲盒（日常购买场景，QPS < 100）

**方案：分布式锁 + MySQL 乐观锁**

```
1. 获取分布式锁 lock:blindbox:stock:{templateId}
2. 执行乐观锁 UPDATE: stock = stock - 1 WHERE id = ? AND stock >= 1
3. 若影响行数 = 0 → 库存不足，抛出 BusinessException
4. 创建订单
5. 释放锁
```

**SQL（乐观锁）：**
```sql
UPDATE blind_box_template
SET stock = stock - 1
WHERE id = ? AND stock >= 1;
```

`stock >= 1` 条件天然防止超卖。

### 8.3 限定盲盒（限时抢购场景，QPS > 1000）

**方案：Redis Lua 预扣库存 + RabbitMQ 异步创建订单**

> 后续活动时升级，详见 `docs/other/redis_rabbitmq_design.md` 9.4 节。

### 8.4 库存恢复

以下场景需要恢复库存（限定盲盒 stock + 1）：
- 订单超时取消（15 分钟未支付）
- 用户主动取消（仅 PENDING 状态）
- 退款处理

---

## ⏰ 九、订单超时处理

### 定时任务：扫描过期订单

**策略：** 每 1 分钟扫描一次

**流程：**
1. 查询 `status = PENDING` 且 `expireAt < NOW()` 的订单
2. 批量更新状态为 CANCELLED
3. 对限定盲盒，恢复库存（stock + 1）
4. 记录日志

**实现位置：** `@Scheduled` 方法，`BlindBoxOrderService.cancelExpiredOrders()`

---

## 🔄 十、事件驱动（未来，RabbitMQ）

> MVP 阶段使用同步调用，后续接入 RabbitMQ 异步处理。
> 参考 `docs/other/redis_rabbitmq_design.md` 第三章节。

### 10.1 订单状态事件

**Exchange/Topic：** `order.status.events`

**触发时机：** 订单创建、支付成功、开盒完成、取消、退款。

**消费者组：**

| 消费者组 | 职责 | 动作 |
|---------|------|------|
| `order-notification-service` | 用户通知 | 发送站内通知 + 短信 |
| `blind-box-generation-service` | 盲盒方案生成 | 根据预选参数生成旅行方案 |
| `inventory-deduction-service` | 库存扣减 | 扣减/恢复 blind_box_template.stock |
| `order-analytics-service` | 业务统计 | 更新日销售额、订单量 |

### 10.2 开盒事件

**Exchange/Topic：** `blindbox.open.events`

**触发时机：** 用户点击开盒。

**消费者：**

| 消费者 | 职责 | 动作 |
|-------|------|------|
| `result-generation-service` | AI 生成方案 | 调用 LLM API 生成旅行方案 |
| `image-generation-service` | 可视化行程图 | **暂不实现**：AI 生成结果图 |
| `notification-service` | 通知用户 | 推送"你的盲盒已就绪！" |

---

## 🛡️ 十一、安全与异常处理

### 11.1 自定义异常类型

| 异常类 | HTTP 状态码 | 使用场景 |
|--------|-----------|---------|
| BusinessException | 400 | 库存不足、订单已过期、模板已下架 |
| ResourceNotFoundException | 404 | 模板/订单/结果不存在 |
| DuplicateResourceException | 409 | 重复开盒、分享码冲突 |

### 11.2 防超卖

- 乐观锁 SQL：`WHERE stock >= 1` 保证原子性
- 分布式锁：`lock:blindbox:stock:{templateId}` 防止并发竞争
- 库存回滚：取消订单时 `stock + 1`

### 11.3 幂等性

- **开盒操作**：若已开盒，直接返回已有结果，不重复生成
- **支付回调**：若订单已支付，直接返回成功
- **取消订单**：若已取消，静默返回

### 11.4 权限控制

| 接口类型 | 认证要求 | 说明 |
|---------|---------|------|
| 盲盒列表/详情 | 无需认证 | 公开展示 |
| 创建订单/查询/开盒 | 需要 JWT | 用户操作 |
| 分享结果查看 | 无需认证 | 公开分享链接 |
| 模板 CRUD | 需要 JWT + ADMIN | 后台管理 |
| 订单管理（全部用户） | 需要 JWT + ADMIN | 后台管理 |

---

## 📊 十二、API 端点汇总

### 前台接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/v1/blind-box` | 否 | 盲盒模板列表（分页） |
| GET | `/api/v1/blind-box/{id}` | 否 | 模板详情 |
| POST | `/api/v1/blind-box/orders` | ✅ | 创建订单（含预选参数） |
| GET | `/api/v1/blind-box/orders` | ✅ | 我的订单列表（分页） |
| GET | `/api/v1/blind-box/orders/{orderNo}` | ✅ | 订单详情 |
| POST | `/api/v1/blind-box/orders/{orderNo}/cancel` | ✅ | 取消订单 |
| POST | `/api/v1/blind-box/orders/{orderNo}/open` | ✅ | 开盒 |
| GET | `/api/v1/blind-box/my` | ✅ | 我的盲盒列表（分页） |
| GET | `/api/v1/blind-box/orders/{orderNo}/result` | ✅ | 结果详情 |
| POST | `/api/v1/blind-box/orders/{orderNo}/share-result` | ✅ | 生成分享链接 |
| GET | `/api/v1/blind-box/share/{shareCode}` | 否 | 查看分享结果 |

### 支付接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/payments/blind-box/callback` | 否 | 模拟支付回调 |

### ~~AI 分析接口（暂不实现）~~

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| ~~POST~~ | ~~`/api/v1/blind-box/analyze-image`~~ | ✅ | **暂不实现**：AI 图片分析 |

### 后台管理接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/v1/admin/blind-box/templates` | ✅ ADMIN | 模板列表 |
| POST | `/api/v1/admin/blind-box/templates` | ✅ ADMIN | 创建模板 |
| PUT | `/api/v1/admin/blind-box/templates/{id}` | ✅ ADMIN | 更新模板 |
| DELETE | `/api/v1/admin/blind-box/templates/{id}` | ✅ ADMIN | 删除模板 |
| GET | `/api/v1/admin/blind-box/orders` | ✅ ADMIN | 全部订单列表（分页） |

---

## 📦 十三、项目文件结构

```
src/main/java/com/ctrip/
└── blindbox/
    ├── controller/
    │   ├── BlindBoxController.java           # 前台盲盒列表/详情
    │   ├── BlindBoxOrderController.java      # 订单创建/查询/取消/开盒
    │   └── AdminBlindBoxController.java      # 后台模板/订单管理
    ├── service/
    │   ├── BlindBoxTemplateService.java      # 模板服务接口
    │   ├── BlindBoxOrderService.java         # 订单服务接口
    │   ├── BlindBoxResultService.java        # 结果服务接口
    │   ├── BlindBoxPreferenceService.java    # 偏好服务接口
    │   ├── BlindBoxSchemeService.java        # 盲盒方案生成接口（AI 预留）
    │   └── impl/
    │       ├── BlindBoxTemplateServiceImpl.java
    │       ├── BlindBoxOrderServiceImpl.java
    │       ├── BlindBoxResultServiceImpl.java
    │       ├── BlindBoxPreferenceServiceImpl.java
    │       └── BlindBoxSchemeServiceImpl.java  # MVP 返回 mock 方案
    ├── mapper/
    │   ├── BlindBoxTemplateMapper.java
    │   ├── BlindBoxOrderMapper.java
    │   ├── BlindBoxResultMapper.java
    │   └── BlindBoxPreferenceMapper.java
    ├── entity/
    │   ├── BlindBoxTemplate.java
    │   ├── BlindBoxOrder.java
    │   ├── BlindBoxResult.java
    │   └── BlindBoxPreference.java
    ├── dto/
    │   ├── CreateOrderRequest.java
    │   ├── OrderResponse.java
    │   ├── TemplateResponse.java
    │   ├── ResultResponse.java
    │   ├── ShareResponse.java
    │   └── SchemeResponse.java
    ├── converter/
    │   ├── TemplateConverter.java
    │   ├── OrderConverter.java
    │   └── ResultConverter.java
    └── enums/
        ├── BlindBoxType.java           # DAILY / LIMITED
        ├── TemplateStatus.java         # ACTIVE / INACTIVE
        ├── OrderStatus.java            # PENDING/PAID/OPENED/REFUNDED/CANCELLED
        └── BudgetLevel.java            # ECONOMY / STANDARD / LUXURY
```

---

## 🎯 十四、盲盒方案生成（MVP 策略）

### 14.1 MVP 实现

> **不依赖 AI**，基于预设规则 + 数据库随机选取：

1. 读取 `blind_box_preference` 中的预选参数（出发地、预算、主题）
2. 根据主题从 `destination` 表筛选匹配的目的地
3. 根据预算等级从关联的 `attraction` 表中选取景点
4. 随机组合生成 3-5 天的行程方案
5. 按 `resultText` JSON 结构组装返回

### 14.2 方案生成接口

封装 `BlindBoxSchemeService` 接口，MVP 返回 mock 数据，后续对接真实大模型 API：

```java
public interface BlindBoxSchemeService {
    /**
     * 根据预选参数生成盲盒旅行方案
     * MVP 返回基于规则的 mock 方案，后续接入 AI API
     */
    SchemeResponse generateScheme(BlindBoxPreference preference);
}
```

### 14.3 ~~图片分析~~（暂不实现）

> **暂不实现**：AI 处理图片提取特征信息的接口尚未实现。
> 封装 `AiService` 接口留作后续对接。

---

## ✅ 十五、验收标准

- [ ] 盲盒列表展示正常，区分日常/限定类型，限定盲盒显示库存
- [ ] 创建订单时预选参数正确存入 `blind_box_preference` 表
- [ ] 日常盲盒不检查库存，限定盲盒正确扣减库存
- [ ] 创建订单 → 模拟支付 → 支付成功流程完整
- [ ] 订单超时（15分钟）自动取消，限定盲盒恢复库存
- [ ] 开盒返回完整旅行方案（目的地 + 行程数据 + 预算）
- [ ] 开盒幂等：重复开盒返回同一结果
- [ ] 盲盒结果可一键导入行程（Phase 3 已有行程模块）
- [ ] ~~结果图可下载~~（**暂不实现**）
- [ ] 结果分享链接可访问，过期后失效（结果图暂不展示）
- [ ] 库存扣减/恢复正确，限定盲盒无超卖
- [ ] 所有接口有单元测试覆盖

---

## 📝 十六、关键技术栈

- **框架：** Spring Boot 3.x
- **ORM：** MyBatis Plus
- **数据库：** MySQL 8.0
- **缓存/锁：** Redis（分布式锁 `lock:blindbox:*`、限流、热点缓存）
- **消息队列：** RabbitMQ（未来，订单/开盒事件）
- **工具：** Lombok, Jakarta Validation
- **调度：** `@Scheduled` 定时任务（订单超时扫描）
- **对象存储：** 本地存储 / 阿里云 OSS（MVP 本地，~~结果图暂不实现~~）
