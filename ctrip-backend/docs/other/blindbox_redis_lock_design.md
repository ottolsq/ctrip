# 盲盒模块 Redis 分布式锁设计

> 基于 Redisson 实现，用于限定盲盒库存扣减和开盒操作的并发安全控制。

---

## 1. 库存扣减锁 `lock:blindbox:stock:{templateId}`

### 1.1 实现代码

```java
private void decrementStockWithLock(Long templateId) {
    String lockKey = "lock:blindbox:stock:" + templateId;
    RLock lock = redissonClient.getLock(lockKey);
    try {
        if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        int rows = templateMapper.decrementStock(templateId);
        if (rows == 0) {
            throw new BusinessException("库存不足");
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException("库存扣减被中断");
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

对应的 SQL（`BlindBoxTemplateMapper:24`）：

```sql
UPDATE blind_box_template 
SET stock = stock - 1 
WHERE id = #{templateId} AND stock >= 1;
```

### 1.2 设计考虑

**为什么要双重保护？**

单看 SQL 本身已经能防超卖（`stock >= 1` 是原子操作），但加分布式锁有三层意义：

| 层次 | 作用 |
|------|------|
| 第 1 层：分布式锁 | 串行化对同一模板的购买请求，减少 DB 并发冲突 |
| 第 2 层：乐观锁 SQL | 兜底，防止锁失效时超卖 |
| 第 3 层：`stock >= 1` 条件 | 即使并发也只有一行能更新成功 |

**锁的粒度按 templateId 而非全局** — 不同模板之间不互相阻塞，只有买同一个限定盲盒的请求才需要串行化。

**超时参数**：`tryLock(3, 10)` — 最多等 3 秒拿不到锁就放弃（10 秒自动过期防死锁），对限定盲盒这种 QPS < 100 的场景足够了。

**释放锁时做了 `isHeldByCurrentThread()` 检查** — 防止极端情况下释放了不属于自己的锁，导致其他线程的锁被提前释放。

---

## 2. 开盒锁 `lock:blindbox:open:{orderId}`

### 2.1 实现代码

```java
public OrderResponse openBox(Long userId, String orderNo) {
    BlindBoxOrder order = requireOrder(orderNo, userId);

    // 仅已支付可开盒
    if (order.getStatus() != OrderStatus.PAID) {
        throw new BusinessException("仅已支付订单可开盒");
    }

    // 获取分布式锁防止重复开盒
    String lockKey = "lock:blindbox:open:" + order.getId();
    RLock lock = redissonClient.getLock(lockKey);
    try {
        if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        // 幂等检查：若已开盒，直接返回
        BlindBoxOrder refreshed = orderMapper.selectById(order.getId());
        if (refreshed.getStatus() == OrderStatus.OPENED) {
            log.info("开盒幂等返回: orderNo={}", orderNo);
            BlindBoxTemplate template = templateMapper.selectById(refreshed.getTemplateId());
            return OrderConverter.toResponse(refreshed, template);
        }

        // 更新状态 → 生成方案 → 保存结果 → 释放锁
        LocalDateTime openedAt = LocalDateTime.now();
        orderMapper.update(null, new LambdaUpdateWrapper<BlindBoxOrder>()
                .eq(BlindBoxOrder::getId, order.getId())
                .set(BlindBoxOrder::getStatus, OrderStatus.OPENED));

        BlindBoxPreference preference = preferenceService.getByOrderId(order.getId());
        SchemeResponse scheme = schemeService.generateScheme(preference);

        String resultText = objectMapper.writeValueAsString(scheme);

        BlindBoxResult result = BlindBoxResult.builder()
                .orderId(order.getId())
                .destination(scheme.destination())
                .destinationId(scheme.destinationId())
                .theme(scheme.theme())
                .resultText(resultText)
                .openedAt(openedAt)
                .build();
        resultMapper.insert(result);

        return OrderConverter.toResponse(orderMapper.selectById(order.getId()), template);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException("开盒操作被中断");
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 2.2 设计考虑

**为什么要用锁？**

开盒不是简单改个状态，它涉及多个步骤：

```
1. 订单状态 PAID → OPENED
2. 调用 schemeService 生成旅行方案
3. 序列化方案为 JSON
4. 插入 blind_box_result 表
5. 记录 openedAt 时间
```

如果两个请求同时到达，没有锁的话会出现：

| 风险 | 后果 |
|------|------|
| 两个方案都被生成 | 浪费资源，且可能结果不同 |
| 两条 result 记录 | `uk_order_id` 唯一约束报错，但第一条可能已写入 |
| 用户看到的结果不一致 | 第二次覆盖了第一次 |

锁 + 幂等检查保证了**同一订单只能开盒一次**：

```
线程 A 拿到锁 → 检查未开盒 → 执行开盒 → 释放锁
线程 B 等锁    → 拿到锁    → 检查已开盒 → 直接返回（幂等）
```

**为什么锁的 key 是 orderId 而不是 orderNo？**

orderId 是数据库自增主键（Long），orderNo 是业务编号（String）。用 orderId 更短、更高效，且在获取锁之前已经通过 `requireOrder()` 查到了订单实体，拿 orderId 零成本。

**为什么在锁内又做了一次 `selectById`？**

获取锁之前的状态判断可能已经过时（锁外检查时是 PAID，拿到锁时可能已被其他线程开盒），所以**锁内必须重新查数据库**做最终判断 — 这就是经典的 check-lock-check 模式。

---

## 3. 对比总结

| 维度 | 库存扣减锁 | 开盒锁 |
|------|-----------|--------|
| 锁的 key | `lock:blindbox:stock:{templateId}` | `lock:blindbox:open:{orderId}` |
| 锁的粒度 | templateId（模板级） | orderId（订单级） |
| 锁的作用 | 防并发超卖 | 防重复开盒 |
| 是否有 DB 乐观锁配合 | 是（`stock >= 1`） | 否（靠状态枚举保证） |
| 幂等策略 | SQL 条件天然幂等 | 锁内二次检查状态 |
| 典型并发场景 | 多人抢同一个限定盲盒 | 网络超时导致用户重复点击开盒 |
| 超时参数 | `tryLock(3, 10)` | `tryLock(3, 10)` |

---

## 4. RabbitMQ 预留设计（MVP 阶段未接入）

> 详见 `docs/blindbox_module.md` 第十章，以下设计在 MVP 阶段暂不实现，保留文档供后续迭代接入。

### 4.1 订单状态事件

**Exchange/Topic：** `order.status.events`

| 消费者组 | 职责 | 动作 |
|---------|------|------|
| `order-notification-service` | 用户通知 | 发送站内通知 + 短信 |
| `blind-box-generation-service` | 盲盒方案生成 | 根据预选参数生成旅行方案 |
| `inventory-deduction-service` | 库存扣减 | 扣减/恢复 blind_box_template.stock |
| `order-analytics-service` | 业务统计 | 更新日销售额、订单量 |

### 4.2 开盒事件

**Exchange/Topic：** `blindbox.open.events`

| 消费者 | 职责 | 动作 |
|-------|------|------|
| `result-generation-service` | AI 生成方案 | 调用 LLM API 生成旅行方案 |
| `image-generation-service` | 可视化行程图 | **暂不实现**：AI 生成结果图 |
| `notification-service` | 通知用户 | 推送"你的盲盒已就绪！" |

### 4.3 高并发库存扣升级方案

> 当限定盲盒 QPS > 1000 时，当前方案升级为 Redis Lua 预扣库存 + RabbitMQ 异步创建订单。

```
1. Redis Lua 脚本预扣库存（原子操作，不经过 DB）
2. 扣减成功 → 发送订单消息到 RabbitMQ → 异步消费创建订单
3. 扣减失败 → 直接返回库存不足
4. 订单超时取消 → 发送恢复库存消息 → Redis Lua 恢复 + DB 同步
```

详见 `docs/other/redis_rabbitmq_design.md` 第 9.4 节。
