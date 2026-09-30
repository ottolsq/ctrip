# 技术深化实施计划

> 四个方向：热点缓存、缓存穿透、Lua秒杀、消息队列深度集成

---

## 总览

```
第 1 轮 ──────► 第 2 轮 ──────► 第 3 轮 ──────► 第 4 轮
热点缓存+       Lua 完整秒杀     MQ 异步开盒      MQ 消费者补全
穿透防护        (依赖 Redis     (依赖第 2 轮     (依赖第 2/3 轮
(无依赖)         操作经验)       MQ 基础设施)     MQ 模式)
```

| 轮次 | 对应功能 | 新建文件 | 修改文件 |
|------|---------|---------|---------|
| 第 1 轮 | 热点缓存 + 缓存穿透 | 2 | 2 |
| 第 2 轮 | 完整秒杀 (Lua) | 4 | 2 |
| 第 3 轮 | 消息队列 (异步开盒) | 3 | 3 |
| 第 4 轮 | 消息队列 (消费者补全) | 3 | 0 |
| **合计** | | **12** | **7** |

---

## 第 1 轮：热点缓存 + 缓存穿透防护

### 目标

让 Redis 不只是"配置了"，而是真实参与热点数据读取，同时防止缓存穿透。

### 涉及文件

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `config/CacheConfig.java` | **新建** | TTL 常量 + Redisson RBloomFilter Bean + @PostConstruct 加载有效 ID |
| 2 | `content/cache/CacheService.java` | **新建** | `getOrLoad()` 通用缓存读 + `evict()`/`evictByPattern()` 缓存失效 + 空值缓存防穿透 |
| 3 | `content/service/impl/DestinationServiceImpl.java` | **修改** | `getDetail()`/`listDestinations()` 接入缓存；`create/update/delete` 后删除缓存 |
| 4 | `content/service/impl/GuideServiceImpl.java` | **修改** | `getDetail()`/`listGuides()` 接入缓存；`create/update/delete` 后删除缓存 |

### 核心逻辑

#### CacheConfig

```
@Configuration
public class CacheConfig {
    // TTL 常量(均带 ±10% 随机偏移防雪崩)
    DEST_DETAIL_TTL = 24h
    DEST_LIST_TTL = 5min
    GUIDE_DETAIL_TTL = 30min
    GUIDE_LIST_TTL = 5min
    ATTRACTION_BY_DEST_TTL = 1h
    NULL_VALUE_TTL = 1min  ← 空值缓存，防穿透关键

    @Bean RBloomFilter<String> destinationBloomFilter
        ① redisson.getBloomFilter("bloom:dest:ids")
        ② tryInit(预期数量=10000, 误判率=0.01)
        ③ @PostConstruct: 从 DB 加载全部有效 ID

    @Bean RBloomFilter<String> guideBloomFilter
        ① redisson.getBloomFilter("bloom:guide:ids")
        ② tryInit(预期数量=50000, 误判率=0.01)
        ③ @PostConstruct: 从 DB 加载全部已发布攻略 ID
}
```

#### CacheService

```
@Service
public class CacheService {
    依赖: StringRedisTemplate, ObjectMapper
    常量: NULL_MARKER = "{\"__null__\":true}"

    <T> T getOrLoad(key, Class<T>, Supplier<T> dbLoader, Duration ttl):
        ① redis.get(key)
           ├── != null → 是 NULL_MARKER ? return null : 反序列化 → return data
           └── == null → dbLoader.get()
               ├── DB 有数据 → redis.set(key, json, ttl+随机偏移) → return data
               └── DB 无数据 → redis.set(key, NULL_MARKER, 1min) → return null

    void evict(key): redis.delete(key)
    void evictByPattern(pattern): keys = redis.keys(pattern); redis.delete(keys)
}
```

#### DestinationServiceImpl 改造示例

```
getDetail(id):
    ① 布隆过滤器检查 "dest:"+id → 不存在则直接抛 ResourceNotFoundException
    ② cacheService.getOrLoad("content:dest:"+id, DestinationResponse.class,
          () -> { 原 DB 查询逻辑 }, DEST_DETAIL_TTL)

createDestination(request):
    ① destinationMapper.insert(destination)
    ② cacheService.evict("content:dest:" + id)        // 如果有直接返回 ID
    ③ cacheService.evictByPattern("content:dest:list:*")
    ④ destinationBloomFilter.add("dest:" + id)
```

### 缓存穿透三层防护

| 层 | 机制 | 位置 | 效果 |
|----|------|------|------|
| 1 | 布隆过滤器 | CacheConfig.init + Service 入口 | 不存在的 ID 直接拦截 |
| 2 | 空值缓存 | CacheService.getOrLoad() | 同一无效 ID 1 分钟内不再穿透 |
| 3 | 参数校验 | DTO @NotNull/@Positive | 拦截 -1、0、null |

### 实施步骤

```
步骤 1-1：新建 CacheConfig.java → 编译通过
步骤 1-2：新建 CacheService.java → 编译通过
步骤 1-3：修改 DestinationServiceImpl.java → 编译通过
步骤 1-4：修改 GuideServiceImpl.java → 编译通过
步骤 1-5：启动应用，调接口验证缓存命中/失效
```

---

## 第 2 轮：Redis Lua 完整秒杀

### 目标

将设计文档中的 Lua 脚本方案落地为可运行代码，与日常购买形成两条独立链路。

### 涉及文件

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `resources/lua/seckill.lua` | **新建** | Lua 脚本：GET stock → SISMEMBER 防重 → DECR + SADD |
| 2 | `blindbox/dto/FlashSaleMessage.java` | **新建** | Record: userId, templateId, orderNo, timestamp |
| 3 | `blindbox/service/FlashSaleService.java` | **新建** | 秒杀入口：执行 Lua → 发送 MQ → 返回状态；预热/结束活动 |
| 4 | `blindbox/messaging/FlashSaleOrderConsumer.java` | **新建** | MQ 消费者：幂等检查 → MySQL 扣库存 → 创建订单；失败回滚 Redis |
| 5 | `config/RabbitMQConfig.java` | **修改** | 新增 flash.sale.exchange + flash.sale.order.queue + 绑定 |
| 6 | `blindbox/service/impl/BlindBoxOrderServiceImpl.java` | **修改** | 新增 prewarmStock() 和 reconcileStock() 定时任务 |

### 两条独立链路

```
日常购买：  POST /api/v1/blind-box/orders
              → BlindBoxOrderServiceImpl.createOrder()
                → 分布式锁 + 乐观锁 → 同步创建订单

秒杀抢购：  POST /api/v1/blind-box/flash-sale
              → FlashSaleService.execute()
                → Lua 原子预扣 → MQ 异步 → 消费者创建订单
```

### 核心逻辑

#### seckill.lua

```lua
-- KEYS[1] = templateId, ARGV[1] = userId
-- 返回：1=成功, 0=库存不足, -1=重复购买

local stockKey = 'seckill:stock:' .. KEYS[1]
local userSetKey = 'seckill:users:' .. KEYS[1]
local stock = tonumber(redis.call('GET', stockKey) or '0')

if stock <= 0 then return 0 end
if redis.call('SISMEMBER', userSetKey, ARGV[1]) == 1 then return -1 end

redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, ARGV[1])
redis.call('EXPIRE', userSetKey, 86400)
return 1
```

#### FlashSaleService

```
execute(userId, templateId):
    ① redisTemplate.execute(seckillScript, [templateId], userId)
    ② result == 1 → 构建 FlashSaleMessage → rabbitTemplate.convertAndSend(
           "flash.sale.exchange", "flash.sale.order", message)
         → return queued(orderNo)
    ③ result == 0 → return soldOut()
    ④ result == -1 → return duplicated()

prewarmStock(templateId, stock, duration):
    redisTemplate.set("seckill:stock:"+templateId, stock, duration)
```

#### FlashSaleOrderConsumer

```
@RabbitListener(queues = "flash.sale.order.queue")
@Transactional
onCreateOrder(msg):
    ① Redis SETNX 幂等检查
    ② templateMapper.decrementStock() → rows==0 时 INCR 回滚 Redis
    ③ orderMapper.insert(order)
    ④ 异常 → INCR 回滚 + throw(触发 MQ 重试)
```

### 实施步骤

```
步骤 2-1：新建 resources/lua/seckill.lua
步骤 2-2：新建 FlashSaleMessage.java
步骤 2-3：新建 FlashSaleService.java → 编译通过
步骤 2-4：修改 RabbitMQConfig.java 新增秒杀 Exchange/Queue/Binding
步骤 2-5：新建 FlashSaleOrderConsumer.java → 编译通过
步骤 2-6：修改 BlindBoxOrderServiceImpl 新增预热/对账方法
步骤 2-7：手动测试：预热 → 秒杀接口 → MQ 消费 → 订单创建
```

---

## 第 3 轮：RabbitMQ 异步开盒（核心结构改造）

### 目标

解决当前最大结构问题：AI 调用在用户请求线程中同步阻塞 5-20 秒。

### 涉及文件

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `blindbox/entity/enums/OrderStatus.java` | **修改** | 新增 PROCESSING(5) |
| 2 | `blindbox/messaging/BlindBoxResultGenConsumer.java` | **新建** | 异步调 AI 生成 + 保存结果 + 更新状态 |
| 3 | `blindbox/messaging/OrderNotificationConsumer.java` | **新建** | 支付成功/开盒完成通知（MVP log 输出） |
| 4 | `blindbox/messaging/InventoryDeductionConsumer.java` | **新建** | 支付成功异步扣 MySQL 库存 |
| 5 | `blindbox/service/impl/BlindBoxOrderServiceImpl.java` | **修改** | openBox() 重构为 requestOpen() |
| 6 | `config/RabbitMQConfig.java` | **修改** | 确认/补全 Queue 绑定 |

### 改造前后对比

```
改造前（同步）：
  POST /open → [加锁→更新状态→同步调AI(5-20s)→保存→解锁] → 返回结果
  用户等待 5-20 秒

改造后（异步）：
  POST /open → [加锁→状态→PROCESSING→发MQ→解锁] → 立即返回(ms级)
                  ↓ MQ 异步
               [消费者→调AI→保存结果→状态→OPENED→发通知]

  前端：轮询 GET /orders/{orderNo} → PROCESSING(等待) / OPENED(展示)
```

### 核心逻辑

#### OrderStatus 新增

```
PENDING(0) → PAID(1) → PROCESSING(5) → OPENED(2)
                           ↓(失败)
                        PAID(1)  ← 回退重试
```

#### BlindBoxOrderServiceImpl 重构

```
requestOpen(userId, orderNo):        ← 同步，毫秒级
    ① 校验 status == PAID
    ② tryLock("lock:blindbox:open:"+orderId)
    ③ 锁内重查状态:
       PAID → 更新为 PROCESSING → 发 MQ → 解锁 → 返回 PROCESSING
       OPENED → 解锁 → 直接返回结果(幂等)
       PROCESSING → 解锁 → 返回 PROCESSING(防重)
    ④ 释放锁 + 立即返回

(原 openBox 中的 AI 调用逻辑移到 BlindBoxResultGenConsumer)
```

#### BlindBoxResultGenConsumer

```
@RabbitListener(queues = "blindbox.result.gen.queue")
@Transactional
onOpenRequested(event):
    ① Redis SETNX 幂等检查
    ② 查询 order(确保 status==PROCESSING)
    ③ preference → schemeService.generateScheme()  ← AI 在这里异步执行
    ④ 保存 BlindBoxResult
    ⑤ 更新 status PROCESSING → OPENED
    ⑥ 发布 ORDER_OPENED + 通知事件
```

### 前端协作

```
1. POST /open → {status:"PROCESSING"}
2. 每 2s 轮询 GET /orders/{orderNo} → PROCESSING("AI生成中...") / OPENED(展示)
3. 30s 超时 → 提示"方案生成中，请稍后刷新"
```

### 实施步骤

```
步骤 3-1：OrderStatus 枚举新增 PROCESSING
步骤 3-2：新建 BlindBoxResultGenConsumer.java
步骤 3-3：新建 OrderNotificationConsumer.java
步骤 3-4：新建 InventoryDeductionConsumer.java
步骤 3-5：重构 BlindBoxOrderServiceImpl.openBox()
步骤 3-6：确认 RabbitMQConfig Queue 绑定完整
步骤 3-7：测试：支付 → 开盒 → MQ 消费 → 轮询结果
```

---

## 第 4 轮：消息队列消费者补全

### 目标

让全部 8 个消费者都有实际业务逻辑。

### 消费者总览

| 消费者 | 队列 | 来源 |
|--------|------|------|
| OrderStatusConsumer | `order.status.queue` | ✅ 已有 |
| UserActivityConsumer | `user.activity.queue` | ✅ 已有 |
| BlindBoxResultGenConsumer | `blindbox.result.gen.queue` | 第 3 轮新建 |
| OrderNotificationConsumer | `order.notification.queue` | 第 3 轮新建 |
| InventoryDeductionConsumer | `inventory.deduction.queue` | 第 3 轮新建 |
| BlindBoxNotificationConsumer | `blindbox.notification.queue` | 本轮新建 |
| AnalyticsConsumer | `analytics.queue` | 本轮新建 |
| ContentAuditConsumer | `content.audit.queue` | 本轮新建 |

### 涉及文件

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `blindbox/messaging/BlindBoxNotificationConsumer.java` | **新建** | 盲盒结果生成完成通知 |
| 2 | `messaging/consumer/AnalyticsConsumer.java` | **新建** | 用户行为统计分析（MVP log 输出） |
| 3 | `messaging/consumer/ContentAuditConsumer.java` | **新建** | 攻略审核事件处理 |

### 核心逻辑

#### BlindBoxNotificationConsumer

```
@RabbitListener(queues = "blindbox.notification.queue")
onResultGenerated(event):
    ① 幂等检查
    ② BLINDBOX_RESULT_GENERATED → log 通知用户方案已生成
    ③ BLINDBOX_SHARED → log 记录分享
```

#### AnalyticsConsumer

```
@RabbitListener(queues = "analytics.queue")
onUserActivity(event):
    ① 幂等检查
    ② 按类型记录：USER_REGISTERED / GUIDE_VIEWED / GUIDE_LIKED /
       ITINERARY_CREATED / BLINDBOX_PURCHASED
    // 后续可写入统计表，用于运营仪表盘
```

#### ContentAuditConsumer

```
@RabbitListener(queues = "content.audit.queue")
onContentAudit(event):
    ① 幂等检查
    ② GUIDE_SUBMITTED → log 攻略提交审核
    ③ GUIDE_APPROVED → log 攻略审核通过
    ④ GUIDE_REJECTED → log 攻略审核拒绝
```

### 实施步骤

```
步骤 4-1：新建 BlindBoxNotificationConsumer.java
步骤 4-2：新建 AnalyticsConsumer.java
步骤 4-3：新建 ContentAuditConsumer.java
步骤 4-4：RabbitMQ 管控台验证 8 个消费者均在线
```

---

## 风险与应对

| 风险 | 应对 |
|------|------|
| 缓存与 DB 不一致 | Cache-Aside 写失效 + TTL 兜底，不追求强一致 |
| 布隆过滤器误判 | 只做拦截不做确认——说不存在才跳过，说可能存在仍走完整路径 |
| Lua 扣库存后 MQ 失败 | 消费者异常时 INCR 回滚 + MQ 重试 3 次 + 每小时对账兜底 |
| 异步开盒前端轮询体验差 | PROCESSING 状态透明展示 + 超时友好提示（后续升级 WebSocket） |
| 现有 createOrder 逻辑受影响 | 秒杀走独立 FlashSaleService，两条链路互不干扰 |
