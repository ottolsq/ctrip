# 行程模块逻辑分析

## 📋 一、整体架构

行程模块采用经典的**三层架构**：

- **Controller层**：处理HTTP请求和响应
- **Service层**：业务逻辑处理
- **Mapper层**：数据访问（MyBatis Plus）

### 核心组件

| 组件 | 路径 | 职责 |
|------|------|------|
| ItineraryController | `/api/v1/itineraries/**` | 行程管理端点（CRUD、收藏） |
| ItineraryDayController | `/api/v1/itineraries/{id}/days/**` | 日程管理端点 |
| ItineraryItemController | `/api/v1/days/{dayId}/items/**` | 行程项管理端点 |
| ItineraryShareController | `/api/v1/itineraries/{id}/share/**` | 分享功能端点 |
| ItineraryService | `com.ctrip.itinerary.service` | 行程业务逻辑 |
| ItineraryDayService | `com.ctrip.itinerary.service` | 日程业务逻辑 |
| ItineraryItemService | `com.ctrip.itinerary.service` | 行程项业务逻辑 |

### 依赖关系

行程模块依赖 Phase 1（用户模块）和 Phase 2 部分（目的地/景点数据）：
- `itinerary.user_id` → `users.id`（FK）
- `itinerary.destination_id` → `destination.id`（FK，可选）
- `itinerary_item` 中的景点可关联 `attraction.id`（通过 type + name 间接关联）

---

## 🗺️ 二、行程管理模块 (Itinerary)

> **注意：** 以下接口均需要JWT认证，从 `SecurityContext` 中获取当前用户ID。

### 1. 创建行程 (`POST /api/v1/itineraries`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 校验起止日期：end_date >= start_date
3. 校验 destination_id 是否存在（若提供）
4. 创建行程主记录（status = DRAFT）
5. 根据起止日期自动创建默认的 ItineraryDay 记录（day_number 从 1 开始递增）
6. 返回创建后的行程详情（含日程列表）

**关键代码位置：** `ItineraryServiceImpl.createItinerary()`

**事务要求：** 行程创建 + 日程初始化需在同一事务内完成。

---

### 2. 我的行程列表 (`GET /api/v1/itineraries`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 查询该用户的所有行程
3. 按 updated_at 降序排序（最近编辑的优先）
4. 支持可选筛选：
   - `status`：按状态筛选（DRAFT/PUBLISHED/ARCHIVED）
   - `destinationId`：按目的地筛选
5. 返回分页结果（只含行程概要，不展开日程明细）

**关键代码位置：** `ItineraryServiceImpl.listItineraries()`

---

### 3. 行程详情 (`GET /api/v1/itineraries/{id}`)

**流程：**

1. 根据 id 查询行程
2. 校验行程存在性
3. 加载关联的 itinerary_day 列表（按 day_number 排序）
4. 加载每个 day 下的 itinerary_item 列表（按 sort_order 排序）
5. 如果是自己的行程 → 返回完整详情
6. 如果是他人的行程 → 仅当 status = PUBLISHED 时可查看，否则返回 403
7. 非创建者访问时 view_count +1

**关键代码位置：** `ItineraryServiceImpl.getDetail()`

---

### 4. 编辑行程 (`PUT /api/v1/itineraries/{id}`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 校验行程存在性
3. 校验权限：仅创建者可编辑
4. 支持部分更新（只更新非 null 字段）：
   - `title`（行程标题）
   - `destinationId`（关联目的地）
   - `startDate` / `endDate`（起止日期，若变更则重新生成日程框架）
   - `status`（状态变更：DRAFT → PUBLISHED → ARCHIVED）
5. 返回更新后的行程详情

**实现细节：**
- 使用 LambdaUpdateWrapper 动态构建 UPDATE 语句
- 若所有字段均为 null，跳过 SQL 执行
- 状态变更校验：ARCHIVED 不可逆回 DRAFT

**关键代码位置：** `ItineraryServiceImpl.updateItinerary()`

---

### 5. 删除行程 (`DELETE /api/v1/itineraries/{id}`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 校验行程存在性
3. 校验权限：仅创建者可删除
4. 在同一事务内级联删除：
   - 先删除 itinerary_item（通过 day 关联）
   - 再删除 itinerary_day
   - 最后删除 itinerary 主记录
5. 返回成功响应

**关键代码位置：** `ItineraryServiceImpl.deleteItinerary()`

**注意：** MyBatis Plus 不原生支持级联删除，需在 Service 层手动处理。

---

### 6. 收藏行程 (`POST /api/v1/itineraries/{id}/collection`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 校验行程存在性
3. 校验行程状态为 PUBLISHED
4. 检查是否已收藏（幂等）
5. 写入 collection 表（target_type = 'itinerary'）
6. 返回成功

**关键代码位置：** `CollectionServiceImpl.toggleCollection()`

---

### 7. 取消收藏 (`DELETE /api/v1/itineraries/{id}/collection`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 查询并删除收藏记录（幂等，不存在时静默处理）
3. 返回成功

**关键代码位置：** `CollectionServiceImpl.toggleCollection()`

---

## 📅 三、日程管理模块 (ItineraryDay)

### 1. 添加日程 (`POST /api/v1/itineraries/{id}/days`)

**流程：**

1. 校验行程存在且属于当前用户
2. 校验 day_number 不与已有日程重复
3. 创建日程记录
4. 返回更新后的行程（含新增日程）

**关键代码位置：** `ItineraryDayServiceImpl.addDay()`

---

### 2. 编辑日程 (`PUT /api/v1/days/{dayId}`)

**流程：**

1. 查询日程记录
2. 校验日程所属行程属于当前用户
3. 更新字段（title、sort_order）
4. 返回更新后的日程

**关键代码位置：** `ItineraryDayServiceImpl.updateDay()`

---

### 3. 删除日程 (`DELETE /api/v1/days/{dayId}`)

**流程：**

1. 查询日程记录
2. 校验日程所属行程属于当前用户
3. 在同一事务内级联删除关联的 itinerary_item
4. 删除日程记录
5. 重新排序后续日程的 day_number（保持连续性，可选）

**关键代码位置：** `ItineraryDayServiceImpl.deleteDay()`

---

## 📌 四、行程项管理模块 (ItineraryItem)

### 1. 添加行程项 (`POST /api/v1/days/{dayId}/items`)

**流程：**

1. 查询日程记录
2. 校验日程所属行程属于当前用户
3. 校验 type 枚举值合法
4. 创建行程项记录（sort_order 自动递增）
5. 返回创建后的行程项

**关键代码位置：** `ItineraryItemServiceImpl.addItem()`

---

### 2. 编辑行程项 (`PUT /api/v1/items/{itemId}`)

**流程：**

1. 查询行程项记录
2. 校验行程项所属日程的行程属于当前用户
3. 支持部分更新：
   - `type`（类型）
   - `name`（名称）
   - `location`（位置）
   - `timeSlot`（时间段）
   - `description`（描述）
   - `sortOrder`（排序）
4. 返回更新后的行程项

**关键代码位置：** `ItineraryItemServiceImpl.updateItem()`

---

### 3. 删除行程项 (`DELETE /api/v1/items/{itemId}`)

**流程：**

1. 查询行程项记录
2. 校验所属日程的行程属于当前用户
3. 删除记录
4. 返回成功

**关键代码位置：** `ItineraryItemServiceImpl.deleteItem()`

---

### 4. 批量排序 (`PUT /api/v1/days/{dayId}/items/reorder`)

**流程：**

1. 查询日程记录
2. 校验日程所属行程属于当前用户
3. 接收 item_id 有序列表 `[id1, id2, id3, ...]`
4. 校验所有 item_id 均属于该日程
5. 批量更新 sort_order（按列表索引 1-based）
6. 返回成功

**关键代码位置：** `ItineraryItemServiceImpl.reorderItems()`

---

## 🔗 五、分享功能模块

### 1. 生成分享链接 (`POST /api/v1/itineraries/{id}/share`)

**流程：**

1. 查询行程记录
2. 校验行程属于当前用户
3. 校验行程状态为 PUBLISHED（未发布的行程不可分享）
4. 生成 6 位 Base62 随机短码（去重校验）
5. 更新行程的 `shareCode` 和 `shareExpiresAt`（默认 7 天过期）
6. 返回分享链接：`/api/v1/share/{shareCode}`

**关键代码位置：** `ItineraryShareServiceImpl.generateShareLink()`

---

### 2. 查看分享行程 (`GET /api/v1/share/{shareCode}`)

**流程：**

1. 根据 shareCode 查询行程
2. 校验分享链接未过期（shareExpiresAt > 当前时间）
3. 无需登录，只读访问
4. 返回行程概览 + 日程列表 + 行程项列表（脱敏：不展示创建者敏感信息）

**关键代码位置：** `ItineraryShareServiceImpl.getSharedItinerary()`

---

### 3. 取消分享 (`DELETE /api/v1/itineraries/{id}/share`)

**流程：**

1. 查询行程记录
2. 校验行程属于当前用户
3. 清空 `shareCode` 和 `shareExpiresAt`
4. 返回成功（幂等，未分享时静默处理）

**关键代码位置：** `ItineraryShareServiceImpl.cancelShare()`

---

## 🗄️ 六、数据模型设计

==行程数据表位于 ./ctrip_itinerary.sql==

### 1. Itinerary 实体（itinerary 表）

**关键字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| userId | Long | 关联的用户ID（FK → users.id） |
| title | String | 行程标题 |
| destinationId | Long | 关联目的地ID（FK → destination.id，可选） |
| startDate | LocalDate | 行程开始日期 |
| endDate | LocalDate | 行程结束日期 |
| status | ItineraryStatus | 行程状态（见下方） |
| shareCode | String | 分享短码（6位 Base62，唯一） |
| shareExpiresAt | LocalDateTime | 分享链接过期时间 |
| viewCount | Integer | 浏览量 |
| likeCount | Integer | 点赞数 |
| createdAt | LocalDateTime | 创建时间（自动填充） |
| updatedAt | LocalDateTime | 更新时间（自动填充） |

**行程状态枚举（ItineraryStatus）：**

| 状态 | 值 | 说明 |
|------|-----|------|
| DRAFT | 0 | 草稿，仅创建者可见 |
| PUBLISHED | 1 | 已发布，他人可查看 |
| ARCHIVED | 2 | 已归档，只读不可编辑 |

---

### 2. ItineraryDay 实体（itinerary_day 表）

**关键字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| itineraryId | Long | 关联的行程ID（FK → itinerary.id） |
| dayNumber | Integer | 第几天（从 1 开始） |
| title | String | 日程标题（如"第一天：抵达上海"） |
| sortOrder | Integer | 排序序号 |

**约束：**
- `itinerary_id + day_number` 联合唯一索引
- 删除行程时级联删除日程

---

### 3. ItineraryItem 实体（itinerary_item 表）

**关键字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| itineraryDayId | Long | 关联的日程ID（FK → itinerary_day.id） |
| type | ItemType | 行程项类型（见下方） |
| name | String | 名称（如"外滩"、"全季酒店"） |
| location | String | 位置/地址 |
| timeSlot | String | 时间段（如"09:00-11:00"） |
| description | String | 详细描述 |
| sortOrder | Integer | 排序序号 |

**行程项类型枚举（ItemType）：**

| 类型 | 值 | 说明 |
|------|-----|------|
| HOTEL | 0 | 酒店住宿 |
| ATTRACTION | 1 | 景点游览 |
| TRANSPORT | 2 | 交通出行 |
| FOOD | 3 | 餐饮美食 |
| ACTIVITY | 4 | 活动体验 |

**约束：**
- 删除日程时级联删除行程项

---

## 🔒 七、权限设计

### 访问权限矩阵

| 操作 | 创建者 | 其他登录用户 | 未登录用户 |
|------|--------|------------|----------|
| 查看 DRAFT 行程 | ✅ | ❌ (403) | ❌ (401) |
| 查看 PUBLISHED 行程 | ✅ | ✅ | ✅ (分享链接) |
| 编辑行程 | ✅ | ❌ (403) | ❌ (401) |
| 删除行程 | ✅ | ❌ (403) | ❌ (401) |
| 收藏行程 | ✅ | ✅ | ❌ (401) |
| 分享管理 | ✅ | ❌ (403) | ❌ (401) |
| 查看分享链接 | - | - | ✅ |

### 权限校验策略

- 所有写操作通过 `@AuthenticationPrincipal Long userId` 获取当前用户
- Service 层校验资源归属：`if (!itinerary.getUserId().equals(userId)) throw new ForbiddenException()`
- 分享链接为只读公开访问，不校验 JWT

---

## 🔄 八、事务管理

### 事务策略

**级联删除（行程 → 日程 → 行程项）：**

```java
@Transactional
public void deleteItinerary(Long itineraryId, Long userId) {
    // 1. 校验权限
    Itinerary itinerary = requireItinerary(itineraryId, userId);
    // 2. 查询所有日程
    List<ItineraryDay> days = dayMapper.selectList(
        Wrappers.lambdaQuery(ItineraryDay.class)
            .eq(ItineraryDay::getItineraryId, itineraryId));
    // 3. 删除所有行程项
    if (!days.isEmpty()) {
        List<Long> dayIds = days.stream().map(ItineraryDay::getId).toList();
        itemMapper.delete(Wrappers.lambdaQuery(ItineraryItem.class)
            .in(ItineraryItem::getItineraryDayId, dayIds));
    }
    // 4. 删除所有日程
    dayMapper.deleteByItineraryId(itineraryId);
    // 5. 删除行程主记录
    itineraryMapper.deleteById(itineraryId);
}
```

**创建行程 + 日程初始化：**

```java
@Transactional
public ItineraryResponse createItinerary(CreateItineraryRequest request, Long userId) {
    // 1. 创建行程主记录
    Itinerary itinerary = Itinerary.builder()...build();
    itineraryMapper.insert(itinerary);
    // 2. 根据起止日期创建日程
    long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    for (int i = 1; i <= days; i++) {
        ItineraryDay day = ItineraryDay.builder()
            .itineraryId(itinerary.getId())
            .dayNumber(i)
            .title("第" + i + "天")
            .sortOrder(i)
            .build();
        dayMapper.insert(day);
    }
    // 3. 返回完整行程
    return getDetail(itinerary.getId(), userId);
}
```

**读操作：**

```java
@Transactional(readOnly = true)
public ItineraryResponse getDetail(Long id, Long userId) { ... }
```

**事务边界：**
- Service 层方法级别
- 级联操作必须在同一事务内

---

## 💡 九、设计模式与最佳实践

### 1. 依赖注入

**方式：** 构造器注入

```java
private final ItineraryMapper itineraryMapper;
private final ItineraryDayMapper dayMapper;
private final ItineraryItemMapper itemMapper;

public ItineraryServiceImpl(
    ItineraryMapper itineraryMapper,
    ItineraryDayMapper dayMapper,
    ItineraryItemMapper itemMapper) {
    this.itineraryMapper = itineraryMapper;
    this.dayMapper = dayMapper;
    this.itemMapper = itemMapper;
}
```

---

### 2. 级联操作处理

MyBatis Plus 不原生支持级联删除，因此：

- 删除操作在 Service 层显式处理
- 使用 `@Transactional` 保证原子性
- 删除顺序：子表 → 父表

---

### 3. 分享短码生成

```java
private String generateShareCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    SecureRandom random = new SecureRandom();
    StringBuilder code = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
        code.append(chars.charAt(random.nextInt(chars.length())));
    }
    // 去重校验
    if (itineraryMapper.existsByShareCode(code.toString())) {
        return generateShareCode(); // 递归重试
    }
    return code.toString();
}
```

---

### 4. DTO 转换

**ItineraryConverter：** 负责 Entity 与 DTO 之间的转换

```java
public static ItineraryResponse toResponse(Itinerary itinerary) {
    return new ItineraryResponse(
        itinerary.getId(),
        itinerary.getTitle(),
        itinerary.getDestinationId(),
        itinerary.getStartDate(),
        itinerary.getEndDate(),
        itinerary.getStatus().name(),
        itinerary.getViewCount(),
        itinerary.getCreatedAt(),
        itinerary.getUpdatedAt()
    );
}
```

不暴露敏感信息（如 userId）。

---

### 5. 防御性编程

**资源归属校验：**

```java
private Itinerary requireItinerary(Long id, Long userId) {
    Itinerary itinerary = itineraryMapper.selectById(id);
    if (itinerary == null) {
        throw new ResourceNotFoundException("行程不存在：id=" + id);
    }
    if (!itinerary.getUserId().equals(userId)) {
        throw new ForbiddenException("无权操作该行程");
    }
    return itinerary;
}
```

---

### 6. 日期校验

```java
private void validateDates(LocalDate start, LocalDate end) {
    if (end.isBefore(start)) {
        throw new BusinessException("结束日期不能早于开始日期");
    }
    if (start.isBefore(LocalDate.now())) {
        log.debug("[CreateItinerary] 行程开始日期在过去 startDate={}", start);
        // MVP 允许创建过去日期的行程（用户可能补录历史行程）
    }
}
```

---

## 📊 十、API 端点汇总

### 行程管理（需要JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries` | 创建行程 |
| GET | `/api/v1/itineraries` | 我的行程列表（分页） |
| GET | `/api/v1/itineraries/{id}` | 行程详情 |
| PUT | `/api/v1/itineraries/{id}` | 编辑行程 |
| DELETE | `/api/v1/itineraries/{id}` | 删除行程 |

### 日程管理（需要JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries/{id}/days` | 添加日程 |
| PUT | `/api/v1/days/{dayId}` | 编辑日程 |
| DELETE | `/api/v1/days/{dayId}` | 删除日程（级联行程项） |

### 行程项管理（需要JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/days/{dayId}/items` | 添加行程项 |
| PUT | `/api/v1/items/{itemId}` | 编辑行程项 |
| DELETE | `/api/v1/items/{itemId}` | 删除行程项 |
| PUT | `/api/v1/days/{dayId}/items/reorder` | 批量排序行程项 |

### 收藏功能（需要JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries/{id}/collection` | 收藏行程 |
| DELETE | `/api/v1/itineraries/{id}/collection` | 取消收藏 |

### 分享功能

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/itineraries/{id}/share` | JWT | 生成分享链接 |
| GET | `/api/v1/share/{shareCode}` | 无需 | 查看分享行程（只读） |
| DELETE | `/api/v1/itineraries/{id}/share` | JWT | 取消分享 |

---

## ✅ 十一、总结

行程模块是连接内容社区和盲盒结果的核心枢纽：

### 功能性
- 行程完整 CRUD 生命周期
- 日程 + 行程项三级嵌套管理
- 批量排序支持
- 分享链接生成与查看
- 级联删除保障数据一致性

### 安全性
- 基于所有权的权限控制
- 分享链接只读、可过期
- DRAFT 状态保护隐私
- 状态流转约束（ARCHIVED 不可逆）

### 可扩展性
- 清晰的三级层次结构（行程 → 日程 → 行程项）
- 行程项类型枚举支持扩展
- 分享短码机制可扩展为独立分享表
- 与盲盒模块的"导入行程"功能对接预留

### 技术亮点
- Service 层级联删除保证数据完整性
- 创建行程时自动初始化日程框架
- 分享短码生成带去重校验
- 事务管理覆盖所有级联操作

---

## 📝 附录：关键技术栈

- **框架：** Spring Boot 3.x
- **安全：** Spring Security + JWT
- **ORM：** MyBatis Plus
- **数据库：** MySQL
- **工具：** Lombok, Jakarta Validation
- **日期处理：** Java 8 Time API (JSR310)
