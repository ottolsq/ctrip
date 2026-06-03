# Redis & RabbitMQ 应用方案

## 一、项目背景

本文档描述 Redis 和消息队列（推荐 RabbitMQ）在携程旅游平台项目中的应用方案，包括应用场景、数据结构设计、实施优先级等。

> **决策：选用 RabbitMQ 而非 Kafka。** 本项目当前规模更适合 RabbitMQ（运维轻量、消息路由模型契合业务、MVP 友好）。当日活达到 10 万+ 或有日志流/事件溯源需求时，再考虑迁移 Kafka。

---

## 二、Redis 应用场景（按优先级排序）

### P1：热点内容缓存（最急需）

#### 现状问题

- `DestinationServiceImpl.getDetail(id)` 每次都查 2 次 SQL（目的地 + 景点列表），无缓存
- `GuideServiceImpl.listGuides(...)` 每次 LIKE 查询 + N 次目的地名称查找
- `GuideServiceImpl.getDetail(id)` 做 3 次 DB 往返（requirePublishedGuide → view_count 更新 → 再次 requirePublishedGuide）
- 同一目的地，每个用户浏览都打 MySQL

#### Key 设计

| Key 模式 | 数据结构 | TTL | 说明 |
|---------|---------|-----|------|
| `content:dest:{id}` | String (JSON) | 24h | 目的地详情（DestinationResponse） |
| `content:dest:list:{params}` | String (JSON) | 5min | 目的地列表页 |
| `content:guide:{id}` | String (JSON) | 30min | 攻略详情（GuideResponse） |
| `content:guide:list:{params}` | String (JSON) | 5min | 攻略列表页 |
| `content:attraction:dest:{destId}` | String (JSON) | 1h | 某目的地的景点列表 |

#### 缓存失效策略

采用 **Cache-Aside 模式**，写操作后主动删除对应 key：

- 管理员更新/删除目的地 → 删除 `content:dest:{id}` + `content:dest:list:*`
- 管理员更新/删除攻略 → 删除 `content:guide:{id}` + `content:guide:list:*`
- 攻略点赞/取消点赞 → 删除 `content:guide:{id}`

#### 涉及文件

- `pom.xml` — 添加 `spring-boot-starter-data-redis`
- `src/main/java/com/ctrip/config/RedisConfig.java`（新建）— Redis 连接、模板、序列化配置
- `src/main/java/com/ctrip/content/service/impl/DestinationServiceImpl.java` — 添加缓存逻辑
- `src/main/java/com/ctrip/content/service/impl/GuideServiceImpl.java` — 添加缓存逻辑

---

### P2：替换 RateLimitInterceptor 的内存泄漏（修复 M-1）

#### 现状问题

`RateLimitInterceptor` 使用 `ConcurrentHashMap<String, Bucket>` 无淘汰机制，长期运行 key 数量线性增长（文档已标注为 M-1 问题）。

#### Redis 方案

使用 **Sorted Set 滑动窗口计数**：

| Key 模式 | 操作 | 限制 | 说明 |
|---------|------|------|------|
| `ratelimit:login:{ip}:{分钟}` | ZADD + ZCARD | 5次/分钟 | 登录接口防暴力破解 |
| `ratelimit:register:{ip}:{分钟}` | ZADD + ZCARD | 3次/分钟 | 注册接口防刷 |
| `ratelimit:sms:{ip}:{分钟}` | ZADD + ZCARD | 2次/分钟 | 短信验证码防刷 |
| `ratelimit:api:{userId}:{窗口}` | ZADD + ZCARD | 自定义 | 全局 API 限流 |
| `ratelimit:guide:like:{userId}:{分钟}` | ZADD + ZCARD | 自定义 | 防点赞刷量 |
| `ratelimit:comment:{userId}:{分钟}` | ZADD + ZCARD | 自定义 | 防评论灌水 |

TTL：120 秒（自动过期，无需手动清理）

#### 涉及文件

- `src/main/java/com/ctrip/user/security/RateLimitInterceptor.java` — 替换 ConcurrentHashMap 为 Redis

---

### P3：JWT Token 黑名单（解决 L-5）

#### 现状问题

Refresh Token 轮换后，旧的 Access Token（JWT）仍然有效 15 分钟（文档已标注为 L-5）。

#### Redis 方案

| Key 模式 | 数据结构 | Value | TTL |
|---------|---------|-------|-----|
| `token:blacklist:{jti}` | String | "revoked" | JWT 剩余有效期 |
| `user:sessions:{userId}` | Hash | `{tokenId}: {deviceInfo, issuedAt, lastActiveAt}` | 动态 |

**使用场景**：

- `refreshToken()` 时：将旧 JWT 的 jti 加入黑名单
- `logout()` 时：将当前 JWT 的 jti 加入黑名单
- `JwtAuthenticationFilter.doFilterInternal()`：接受 token 前检查黑名单

#### 涉及文件

- `src/main/java/com/ctrip/user/security/JwtAuthenticationFilter.java` — 添加黑名单检查
- `src/main/java/com/ctrip/user/service/AuthServiceImpl.java` — refresh/logout 时写入黑名单

---

### P4：盲盒分布式锁（未来，盲盒模块实现时）

| Key 模式 | 用途 | 说明 |
|---------|------|------|
| `lock:blindbox:stock:{templateId}` | 盲盒库存扣减 | 防止并发超卖 |
| `lock:blindbox:open:{orderId}` | 开盒操作 | 防止重复开盒 |
| `lock:itinerary:sharecode:{code}` | 分享码生成 | 防短码冲突 |

使用 `SET NX PX` 或 Redisson 实现。

---

### P5：计数器与排行榜

#### 现状问题

- `guide.like_count` / `guide.view_count` 通过 `setSql("like_count = like_count + 1")` 直接更新 MySQL，每次点赞/浏览都写 DB
- 缺少热门攻略/行程排行功能

#### Redis 方案

| Key 模式 | 数据结构 | 操作 | 说明 |
|---------|---------|------|------|
| `counter:guide:views:{id}` | String | INCR | 攻略浏览量 |
| `counter:guide:likes:{id}` | String | INCR/DECR | 攻略点赞数 |
| `counter:itinerary:views:{id}` | String | INCR | 行程浏览量 |
| `leaderboard:guide:weekly` | ZSET | ZADD / ZREVRANGE | 周热门攻略排行 |
| `leaderboard:guide:monthly` | ZSET | ZADD / ZREVRANGE | 月热门攻略排行 |
| `leaderboard:itinerary:hot` | ZSET | ZADD / ZREVRANGE | 热门行程排行 |

**刷库策略**：定时任务每 5 分钟将 Redis 计数器批量刷回 MySQL，应用关闭时全量刷回。

---

### P6：短信/邮箱验证码缓存（修复 M-2）

#### 现状问题

`StubSmsServiceImpl` 使用内存 `ConcurrentHashMap` 存储验证码，非原子操作（文档标注为 M-2），且服务重启后数据丢失。

#### Redis 方案

| Key 模式 | 数据结构 | TTL | 说明 |
|---------|---------|-----|------|
| `auth:otp:sms:{phone}` | String | 5min | 手机验证码 |
| `auth:otp:email:{email}` | String | 5min | 邮箱验证码 |
| `auth:otp:send:{phone}` | String (INCR) | 60s | 发送频率限制（1次/分钟） |

操作：`SET EX` 原子写入，`GET + DEL` 原子验证。

#### 涉及文件

- `src/main/java/com/ctrip/user/service/StubSmsServiceImpl.java` — 替换 ConcurrentHashMap 为 Redis

---

## 三、RabbitMQ 应用场景

> 以下 Topic 设计同样适用于 Kafka。若未来需要迁移，topic 命名可保持不变。

### P1：订单状态事件（核心业务）

**Exchange/Topic**：`order.status.events`

**触发时机**：盲盒订单创建、支付回调确认、开盒完成、退款、取消。

#### 消息格式

```json
{
  "eventId": "evt-abc123",
  "eventType": "ORDER_CREATED | ORDER_PAID | ORDER_OPENED | ORDER_REFUNDED | ORDER_CANCELLED",
  "orderId": 12345,
  "orderNo": "BB20260601001234",
  "userId": 67890,
  "templateId": 101,
  "payAmount": 99.00,
  "payMethod": "ALIPAY | WECHAT_PAY",
  "timestamp": "2026-06-01T10:30:00+08:00",
  "metadata": {
    "payTime": "2026-06-01T10:31:00+08:00",
    "transactionId": "wx_t123456"
  }
}
```

#### 消费者组

| 消费者组 | 职责 | 动作 |
|---------|------|------|
| `order-notification-service` | 用户通知 | 发送站内通知 + 短信 |
| `blind-box-generation-service` | 盲盒方案生成 | 调用 AI 生成旅行方案 |
| `order-analytics-service` | 业务统计 | 更新日销售额、订单量 |
| `inventory-deduction-service` | 库存扣减 | 扣减 `blind_box_template.stock` |

#### 重试/容错

- 死信队列：`order.status.events.dlq`
- 重试策略：3 次，指数退避（1s → 5s → 30s）
- 幂等性：消费者处理前检查 `eventId` 是否已消费

---

### P2：盲盒开盒事件

**Exchange/Topic**：`blindbox.open.events`

**触发时机**：用户点击开盒。

#### 消息格式

```json
{
  "eventId": "evt-xyz789",
  "eventType": "BLINDBOX_OPEN_REQUESTED | BLINDBOX_RESULT_GENERATED | BLINDBOX_SHARED",
  "orderId": 12345,
  "userId": 67890,
  "resultId": 5678,
  "timestamp": "2026-06-01T14:00:00+08:00",
  "payload": {
    "destination": "成都",
    "theme": "美食",
    "budgetLevel": "STANDARD",
    "days": 4
  }
}
```

#### 消费者

| 消费者 | 职责 | 动作 |
|-------|------|------|
| `result-generation-service` | AI 生成方案 | 调用 LLM API，保存到 `blind_box_result` |
| `image-generation-service` | 可视化行程图 | 调用 AI 生成图片，上传 OSS，保存 URL |
| `notification-service` | 通知用户 | 推送"你的盲盒已就绪！" |

---

### P3：用户行为事件

**Exchange/Topic**：`user.activity.events`

**触发时机**：用户注册、浏览攻略、点赞、创建行程、购买盲盒、分享行程等。

#### 消息格式

```json
{
  "eventId": "evt-act456",
  "eventType": "USER_REGISTERED | GUIDE_VIEWED | GUIDE_LIKED | ITINERARY_CREATED | BLINDBOX_PURCHASED | ITINERARY_SHARED",
  "userId": 67890,
  "targetId": 12345,
  "targetType": "GUIDE | ITINERARY | BLINDBOX",
  "timestamp": "2026-06-01T09:00:00+08:00",
  "metadata": {
    "guideId": 1001,
    "destinationId": 5,
    "source": "homepage"
  }
}
```

#### 消费者

| 消费者 | 职责 | 动作 |
|-------|------|------|
| `analytics-service` | 行为分析 | 更新仪表盘、转化漏斗 |
| `recommendation-service` | 个性化推荐 | 构建用户画像，推荐匹配盲盒 |
| `growth-service` | 用户增长 | 触发新人优惠券、里程碑奖励 |

---

### P4：内容审核事件

**Exchange/Topic**：`content.audit.events`

**触发时机**：攻略提交审核、审核通过、审核拒绝。

#### 消息格式

```json
{
  "eventId": "evt-audit789",
  "eventType": "GUIDE_SUBMITTED | GUIDE_APPROVED | GUIDE_REJECTED",
  "guideId": 12345,
  "authorId": 67890,
  "operatorId": 1,
  "reason": "内容违反发布规范",
  "timestamp": "2026-06-01T11:00:00+08:00"
}
```

---

## 四、实施优先级与时间线

### 第一阶段：Redis 基础（2-3 天）

**理由**：解决现有代码的已知问题。

| 优先级 | 内容 | 对应文档问题编号 |
|-------|------|----------------|
| 1 | 热点内容缓存（目的地、攻略） | 新增 |
| 2 | 替换 RateLimitInterceptor 内存泄漏 | M-1 |
| 3 | 替换 StubSmsServiceImpl 内存存储 | M-2 |

**交付物**：
- `RedisConfig.java` 配置类
- 目的地/攻略缓存逻辑
- Redis 限流器
- Redis OTP 存储

### 第二阶段：Redis 进阶（1-2 天）

| 优先级 | 内容 | 对应文档问题编号 |
|-------|------|----------------|
| 4 | JWT Token 黑名单 | L-5 |
| 5 | 分布式锁（盲盒库存/开盒） | 新增 |
| 6 | 计数器与排行榜 | 新增 |

### 第三阶段：消息队列（盲盒模块完成后，3-4 天）

**理由**：现有模块（用户、内容、行程）均为同步调用，异步需求在盲盒订单和支付回调时才出现。

| 优先级 | 内容 |
|-------|------|
| 1 | 订单状态事件流（核心） |
| 2 | 盲盒开盒事件流 |
| 3 | 用户行为事件流 |
| 4 | 内容审核事件流 |

**交付物**：
- `MessagingConfig.java` 配置类
- EventPublisher 通用事件发布器
- 订单/盲盒消费者
- 死信队列与重试机制

---

## 五、整体架构图

```
                         ┌─────────────────────────┐
                         │      UniApp 前端         │
                         │  (PC + 移动端, CDN)      │
                         └───────────┬─────────────┘
                                     │ HTTPS
                                     ▼
                         ┌─────────────────────────┐
                         │     Nginx / Gateway      │
                         │  (反向代理, SSL, 限流)   │
                         └───────────┬─────────────┘
                                     │
                                     ▼
              ┌──────────────────────────────────────────────┐
              │              Spring Boot 应用                 │
              │                                              │
              │  Controllers 层                               │
              │  AuthCtrl | UserCtrl | DestCtrl | GuideCtrl   │
              │  CommentCtrl | ItineraryCtrl | AdminCtrl      │
              │  (未来) BlindBoxCtrl | OrderCtrl              │
              │                                              │
              │  Security 层                                 │
              │  JwtAuthenticationFilter (→ Redis 黑名单检查)  │
              │  RateLimitInterceptor (→ Redis 限流)          │
              │                                              │
              │  Service 层                                  │
              │  GuideService  ──读──→ Redis 缓存查询         │
              │  DestService   ──读──→ Redis 缓存查询         │
              │  BlindBoxService ──锁──→ Redis 分布式锁       │
              │  OrderService  ──发布──→ RabbitMQ             │
              │                                              │
              │  Event Publisher  (未来)                      │
              │  OrderEventPublisher → order.status.events    │
              │  BlindBoxEventPublisher → blindbox.open.events│
              │  ActivityEventPublisher → user.activity.events│
              │                                              │
              │  Event Consumer  (未来)                       │
              │  NotificationConsumer ← order.status.events   │
              │  ResultGenConsumer ← blindbox.open.events     │
              │  AnalyticsConsumer ← user.activity.events     │
              └──────────────────────────────────────────────┘
                         │              │              │
          ┌──────────────┼──────────────┼──────────────┼──────────────┐
          ▼              ▼              ▼              ▼
      ┌────────┐    ┌────────┐    ┌──────────┐   ┌─────────────┐
      │ MySQL  │    │ Redis  │    │RabbitMQ  │   │ 阿里云 OSS  │
      │        │    │        │    │          │   │             │
      │users   │    │热点缓存 │    │order.st  │   │ 攻略封面图  │
      │tokens  │    │限流    │    │blindbox  │   │ 盲盒结果图  │
      │dest    │    │黑名单  │    │user.act  │   │ 行程分享图  │
      │guide   │    │OTP     │    │content.a │   │ 用户头像    │
      │comment │    │分布式锁│    │          │   │             │
      │itinerary│   │计数器  │    │          │   │             │
      │collection│  │排行榜  │    │          │   │             │
      │(future) │    │        │    │          │   │             │
      │blindbox│    │        │    │          │   │             │
      │order   │    │        │    │          │   │             │
      └────────┘    └────────┘    └──────────┘   └─────────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │   异步 Worker    │
                                                │                 │
                                                │ 通知服务          │
                                                │ AI 生成服务      │
                                                │ 数据分析服务      │
                                                │ 图片生成(OSS)    │
                                                └─────────────────┘
```

---

## 六、关键数据流示例

### 数据流 1：攻略详情（含缓存）

```
GET /api/v1/guides/{id}
  → GuideController.getDetail()
    → GuideServiceImpl.getDetail(id)
      → Redis: GET content:guide:{id}
        ├── 命中 → 返回缓存 JSON (~5ms)
        └── 未命中 → 查询 MySQL (guides + destination)
                     → 转换为 DTO
                     → Redis: SET content:guide:{id} EX 1800 (30分钟)
                     → Redis: INCR counter:guide:views:{id}
                     → 返回响应 (~50ms)
```

### 数据流 2：盲盒购买（未来，含消息队列）

```
POST /api/v1/blindbox/orders
  → BlindBoxController.createOrder()
    → BlindBoxService.createOrder(userId, request)
      → Redis: 获取锁 lock:blindbox:stock:{templateId}
      → 检查 blind_box_template.stock > 0
      → 创建 blind_box_order (status=PENDING)
      → Redis: 释放锁
      → 发布事件到 RabbitMQ: order.status.events (ORDER_CREATED)
      → 返回 orderId 给前端

  ← 支付回调（微信/支付宝）
    → PaymentController.callback()
      → 验证签名、幂等检查
      → 更新 blind_box_order 状态为 PAID
      → 发布事件到 RabbitMQ: order.status.events (ORDER_PAID)

  ← RabbitMQ 消费者（异步，独立线程/进程）
    → OrderPaidConsumer.onMessage(event)
      → 扣减模板库存
      → 触发 AI 生成（调用 LLM API）
      → 保存结果到 blind_box_result
      → 发布事件: blindbox.open.events (BLINDBOX_RESULT_GENERATED)
      → 发送通知给用户
```

### 数据流 3：Redis 限流

```
POST /api/v1/auth/login
  → RateLimitInterceptor.preHandle()
    → Key: ratelimit:login:{clientIp}:{当前分钟}
    → Redis: ZADD key {timestamp} {唯一标识}
    → Redis: ZCARD key
      ├── <= 5 → 放行（继续到 Controller）
      └── > 5 → 返回 429 Too Many Requests
    → Redis: EXPIRE key 120 (自动清理)
```

---

## 七、依赖清单

### Redis

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### RabbitMQ

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## 八、配置文件示例

### application.properties（Redis）

```properties
# Redis 连接配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=3000ms
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2

# Redis 序列化
app.redis.key-prefix=ctrip:
```

### application.properties（RabbitMQ）

```properties
# RabbitMQ 连接配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.listener.simple.concurrency=2
spring.rabbitmq.listener.simple.max-concurrency=10
spring.rabbitmq.listener.simple.prefetch=10
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.initial-interval=1000
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.max-interval=30000

# 死信队列
spring.rabbitmq.listener.simple.dead-letter-exchange=dlx.exchange
```

---

## 九、秒杀 / 高并发库存扣减方案

### 9.1 场景分析



我们这里可以增加盲盒类别，日常盲盒和限定盲盒：

日常盲盒不限量，限定盲盒限量（有价格优惠，数量少，限时开放，需要用户抢，？是地点或主题限定？）



本项目的盲盒库存扣减与商品秒杀**高度相似**：

```
盲盒购买 → 扣减 blind_box_template.stock
         → 并发时可能超卖、DB 行锁竞争
```

**秒杀的核心挑战**：

```
10 万用户同时抢 100 个商品
→ 数据库行锁竞争（InnoDB 行级锁阻塞）
→ 超卖（多个线程同时读到库存 > 0）
→ 数据库被打挂（瞬间 QPS 峰值）
```

### 9.2 完整方案：四层防御

Redis 是库存判断的核心，但完整方案需要四层配合：

```
┌──────────────────────────────────────────────────────┐
│                    秒杀完整链路                        │
│                                                      │
│  用户请求                                              │
│    ↓                                                  │
│  ┌─────────────────────────────────┐                 │
│  │ 1. 网关限流 (Sentinel/Nginx)     │ ← 拦截 90% 流量 │
│  │    只放行 1000 QPS               │                 │
│  └────────────────┬────────────────┘                 │
│                   ↓                                  │
│  ┌─────────────────────────────────┐                 │
│  │ 2. Redis 预扣库存 (DECR/Lua)     │ ← 原子操作      │
│  │    返回 success/fail            │   挡掉无库存请求 │
│  └────────────────┬────────────────┘                 │
│                   ↓ (仅成功的)                         │
│  ┌─────────────────────────────────┐                 │
│  │ 3. MQ 异步创建订单              │ ← 削峰填谷       │
│  │    不阻塞用户响应                │                 │
│  └────────────────┬────────────────┘                 │
│                   ↓ (异步)                            │
│  ┌─────────────────────────────────┐                 │
│  │ 4. MySQL 持久化订单 + 扣真实库存 │ ← 保证数据一致  │
│  └─────────────────────────────────┘                 │
└──────────────────────────────────────────────────────┘
```

各层职责：

| 层级 | 技术 | 职责 | 不做会怎样 |
|------|------|------|-----------|
| **限流层** | Sentinel / Nginx | 只放行部分请求，其余直接拒绝 | DB 直接被打挂 |
| **库存层** | **Redis DECR / Lua** | 原子扣减库存，返回是否抢成功 | 超卖、DB 行锁竞争 |
| **削峰层** | RabbitMQ / Kafka | 异步创建订单，平滑写 DB 压力 | 同步写 DB 瞬间阻塞 |
| **持久层** | MySQL | 最终订单落地，数据兜底 | 数据丢失 |

### 9.3 Redis 库存扣减实现

#### 方案 A：DECR（简单场景，需补偿）

```java
// 预热：活动开始前，将库存加载到 Redis
redisTemplate.opsForValue().set("seckill:stock:101", 100);

// 秒杀时：
Long remaining = redisTemplate.opsForValue().decrement("seckill:stock:101");
if (remaining >= 0) {
    // 抢到了！发送 MQ 消息创建订单
    mqTemplate.convertAndSend("seckill.order", userId + ":" + goodsId);
    return "排队中，请等待结果...";
} else {
    // 库存回滚（因为 DECR 已经减了）
    redisTemplate.opsForValue().increment("seckill:stock:101");
    return "已售罄";
}
```

**问题**：DECR 是原子操作，但"扣减 + 判断 + MQ 发送"不是原子操作。如果 MQ 发送失败，需要补偿回滚。

#### 方案 B：Lua 脚本（推荐，事务原子性）

创建 `seckill.lua`：

```lua
-- seckill.lua
local key = 'seckill:stock:' .. KEYS[1]
local stock = tonumber(redis.call('GET', key))
if stock > 0 then
    redis.call('DECR', key)
    return 1  -- 成功
else
    return 0  -- 失败
end
```

Java 调用：

```java
Long result = redisTemplate.execute(
    new DefaultRedisScript<>(luaScript, Long.class),
    Collections.singletonList(goodsId)
);
if (result == 1) {
    // 成功，发 MQ
    mqTemplate.convertAndSend("seckill.order", userId + ":" + goodsId);
    return "排队中，请等待结果...";
} else {
    return "已售罄";
}
```

Lua 脚本在 Redis 中是**原子执行**的，不存在并发安全问题。

### 9.4 本项目适配：盲盒库存扣减

本项目用**分布式锁 + MySQL 乐观锁**即可满足当前并发量级，不需要完整的秒杀方案。

**判断标准**：

| 场景 | 方案 |
|------|------|
| 盲盒日常购买（QPS < 100） | 分布式锁 + MySQL 乐观锁 |
| 盲盒限时抢购（QPS > 1000） | 完整秒杀方案（Redis 预扣 + MQ 削峰） |

#### 日常方案：分布式锁 + 乐观锁

```java
// BlindBoxServiceImpl.createOrder()
public OrderResult createOrder(Long userId, CreateOrderRequest req) {
    // 1. 获取分布式锁
    String lockKey = "lock:blindbox:stock:" + req.getTemplateId();
    RLock lock = redissonClient.getLock(lockKey);
    try {
        if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        // 2. 乐观锁扣减库存
        int rows = blindBoxTemplateMapper.updateStock(
            req.getTemplateId(), 1  // stock = stock - 1 WHERE stock >= 1
        );
        if (rows == 0) {
            throw new BusinessException("库存不足");
        }

        // 3. 创建订单（同步）
        BlindBoxOrder order = buildOrder(userId, req);
        blindBoxOrderMapper.insert(order);

        return OrderResult.success(order);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

对应的 SQL（乐观锁）：

```sql
UPDATE blind_box_template
SET stock = stock - 1
WHERE id = ? AND stock >= 1;
```

`stock >= 1` 条件天然防止超卖，即使并发执行也只会有一行更新成功。

#### 升级方案：限时抢购活动

当盲盒做限时抢购活动时，替换为 Redis Lua 预扣库存方案：

```java
public OrderResult flashSale(Long userId, FlashSaleRequest req) {
    // 1. Redis Lua 预扣库存
    Long result = redisTemplate.execute(
        flashSaleLuaScript,
        Collections.singletonList(req.getTemplateId().toString())
    );
    if (result != 1) {
        return OrderResult.fail("已售罄");
    }

    // 2. 发送 MQ 异步创建订单
    FlashSaleMessage msg = new FlashSaleMessage();
    msg.setUserId(userId);
    msg.setTemplateId(req.getTemplateId());
    msg.setOrderId(generateOrderId());
    mqTemplate.convertAndSend("seckill.blindbox", msg);

    // 3. 立即返回排队状态
    return OrderResult.success("排队中", msg.getOrderId());
}
```

MQ 消费者异步创建订单 + 扣 MySQL 库存：

```java
@RabbitListener(queues = "seckill.blindbox.queue")
public void onCreateOrder(FlashSaleMessage msg) {
    // 1. 幂等检查
    if (orderMapper.existsByOrderId(msg.getOrderId())) {
        return;
    }

    // 2. 创建订单（同步到 MySQL）
    BlindBoxOrder order = new BlindBoxOrder();
    order.setOrderId(msg.getOrderId());
    order.setUserId(msg.getUserId());
    order.setTemplateId(msg.getTemplateId());
    order.setStatus("PENDING");
    orderMapper.insert(order);

    // 3. 更新 Redis 库存（MySQL 扣减，作为最终一致性校验）
    blindBoxTemplateMapper.updateStock(msg.getTemplateId(), 1);
}
```

### 9.5 库存预热与对账

#### 活动预热

```java
// 管理员后台：活动开始前预热库存到 Redis
public void prewarmStock(Long templateId) {
    BlindBoxTemplate template = templateMapper.selectById(templateId);
    String key = "seckill:stock:" + templateId;
    redisTemplate.opsForValue().set(key, template.getStock());

    // 设置活动过期时间（自动清理）
    redisTemplate.expire(key, template.getActivityDuration(), TimeUnit.MINUTES);
}
```

#### 库存对账

```java
// 定时任务：每小时对比 Redis 与 MySQL 库存
@Scheduled(cron = "0 0 * * * ?")
public void reconcileStock() {
    List<BlindBoxTemplate> templates = templateMapper.selectActive();
    for (BlindBoxTemplate t : templates) {
        String key = "seckill:stock:" + t.getId();
        Long redisStock = redisTemplate.opsForValue().get(key);

        if (redisStock != null && !redisStock.equals(t.getStock())) {
            log.warn("库存不一致: templateId={}, redis={}, mysql={}",
                t.getId(), redisStock, t.getStock());

            // 以 MySQL 为准，修正 Redis
            redisTemplate.opsForValue().set(key, t.getStock());
        }
    }
}
```

### 9.6 方案对比总结

| 维度 | 分布式锁 + 乐观锁 | Redis Lua + MQ 完整方案 |
|------|------------------|------------------------|
| 适用 QPS | < 100 | > 1000 |
| 超卖风险 | 无（乐观锁保证） | 无（Lua 原子保证） |
| 响应延迟 | ~50ms | ~5ms（异步排队） |
| 实现复杂度 | 低 | 中 |
| 依赖组件 | Redis（锁）+ MySQL | Redis + RabbitMQ + MySQL |
| 本项目当前推荐 | ✅ 日常购买 | ⏳ 限时抢购活动时升级 |
