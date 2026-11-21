# 趣行旅行平台 管理端 API（P1.1 + P1.2 + P1.3 + P1.4）

> 前缀：`/api/v1/admin`
> 认证：JWT Bearer Token；用户管理/盲盒模板/订单管理/轮播图/推荐位/数据概览要求 `ADMIN`，攻略审核/评论管理/内容数据允许 `ADMIN` 与 `CONTENT_OPERATOR`。
> 响应：统一 `ApiResponse<T>`（`success/data/error`）。

## 1. 权限与种子账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin@example.com | Admin12345 | ADMIN | 全部管理功能 |
| operator@example.com | Operator12345 | CONTENT_OPERATOR | 内容运营（P1.2 起开放） |

种子账号由 `docs/SQL/ctrip_admin_seed.sql` 写入，仅限本地开发/答辩演示。

- `/api/v1/admin/**` 的 URL 基线允许 ADMIN/CONTENT_OPERATOR；
- 用户管理、盲盒模板、订单管理、轮播图、推荐位、数据概览端点额外使用 `@PreAuthorize("hasRole('ADMIN')")`，内容运营/普通用户访问返回 **403**。

## 2. 用户列表

`GET /api/v1/admin/users`

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 10，最大 100 |
| status | string | 否 | `UNVERIFIED` / `ACTIVE` / `SUSPENDED` / `DELETED` |
| role | string | 否 | `USER` / `ADMIN` / `CONTENT_OPERATOR` |
| keyword | string | 否 | 用户名/邮箱/手机号模糊匹配 |
| startDate | date | 否 | 注册日期起（`yyyy-MM-dd`） |
| endDate | date | 否 | 注册日期止（`yyyy-MM-dd`） |

### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 2,
        "username": "user2",
        "email": "user2@example.com",
        "phone": "13800000002",
        "avatarUrl": "https://img.example.com/2.jpg",
        "role": "USER",
        "status": "ACTIVE",
        "createdAt": "2026-01-01T10:00:00",
        "lastLoginAt": null
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  },
  "error": null
}
```

## 3. 用户详情

`GET /api/v1/admin/users/{id}`

响应在列表字段基础上增加：

| 字段 | 类型 | 说明 |
|------|------|------|
| statusBeforeSuspend | string | 封禁前状态，非封禁状态为 null |
| guideCount | number | 用户发布的攻略数 |
| orderCount | number | 用户盲盒订单数 |

## 4. 封禁用户

`POST /api/v1/admin/users/{id}/ban`

- 成功返回 `{ "success": true, "data": null }`。
- 规则：
  - 不能封禁自己；
  - 不能封禁 `ADMIN` 账号；
  - `DELETED` 用户不可封禁；
  - 已封禁用户重复调用为幂等成功。
- 副作用：
  - `status` 置为 `SUSPENDED`，`status_before_suspend` 记录原状态；
  - 删除该用户全部 refresh token；
  - 写入 Redis `auth:deny:user:{userId}`，TTL = access token 有效期（当前 900000ms）；
  - JWT 过滤器命中该 key 时立即返回 **401**，无需等待 access token 过期。

## 5. 解封用户

`POST /api/v1/admin/users/{id}/unban`

- 成功返回 `{ "success": true, "data": null }`。
- 恢复 `status_before_suspend`（为空时恢复 `ACTIVE`），清空该列；
- 删除 Redis 封禁键；
- 非封禁用户调用为幂等成功（仍会清理 Redis 键）。

## 6. 错误码

| HTTP | 场景 |
|------|------|
| 400 | 封禁自己 / 封禁管理员 / 已删除用户 |
| 401 | 未登录、token 无效/过期、账号被封禁 |
| 403 | 非 ADMIN 访问管理端 |
| 404 | 用户不存在 |
| 500 | 未预期异常 |

## 7. 说明

- 封禁后旧 access token 立即失效；解封后旧 token 若仍在有效期内可恢复使用。
- Redis 暂不可用时，封禁检查放行并记录告警（可用性优先）；refresh token 已删除，用户最长 15 分钟后自然失效。
- 用户响应绝不包含 `passwordHash`。

## 8. 攻略审核（ADMIN / CONTENT_OPERATOR）

### 审核列表

`GET /api/v1/admin/guides?page&size&status&keyword&authorId`

- `status`：`DRAFT` / `PENDING` / `PUBLISHED` / `REJECTED`
- `keyword`：标题模糊匹配
- 响应项：`id, authorId, authorName, title, status, rejectReason, viewCount, likeCount, createdAt, updatedAt`

### 审核详情

`GET /api/v1/admin/guides/{id}`

返回完整攻略（含正文、驳回理由），不经过公共缓存/布隆过滤器。

### 通过审核

`POST /api/v1/admin/guides/{id}/approve`

- 仅 `PENDING` 可操作；使用 `WHERE status=PENDING` 条件更新，并发下后到者返回 **400**；
- 状态改为 `PUBLISHED`，记录审核人与时间，清空驳回理由；
- 加入布隆过滤器并清理攻略缓存；
- 事务提交后发送 `GUIDE_APPROVED`。

### 驳回审核

`POST /api/v1/admin/guides/{id}/reject`

请求体：

```json
{ "reason": "内容不完整，请补充交通与住宿信息" }
```

- `reason` 必填，最长 500；
- 仅 `PENDING` 可驳回；条件更新 0 行（并发）返回 **400**；
- 状态改为 `REJECTED`，写入理由/审核人/时间；
- 清理攻略缓存；事务提交后发送 `GUIDE_REJECTED`。

## 9. 评论管理（ADMIN / CONTENT_OPERATOR）

`GET /api/v1/admin/comments?page&size&guideId&userId&keyword&startDate&endDate`

响应项：`id, guideId, guideTitle, userId, userName, userAvatar, parentId, content, createdAt`。

`DELETE /api/v1/admin/comments/{id}`

- 删除任意评论，级联删除其全部子回复；
- 清理对应攻略详情/列表缓存。

## 10. 内容数据（ADMIN / CONTENT_OPERATOR）

`GET /api/v1/admin/content/stats`

```json
{
  "totalGuides": 10,
  "draftCount": 2,
  "pendingCount": 1,
  "publishedCount": 6,
  "rejectedCount": 1,
  "totalViews": 1200,
  "totalLikes": 88,
  "totalComments": 35,
  "topGuides": [
    { "id": 1, "title": "杭州三日游", "viewCount": 500, "likeCount": 30 }
  ],
  "last7Days": [
    { "date": "2026-11-15", "count": 1 }
  ]
}
```

`last7Days` 固定 7 项，缺失日期补 0。

## 11. 盲盒模板管理（ADMIN）

### 模板列表

`GET /api/v1/admin/blind-box/templates?page&limit&type&status&keyword`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| limit | int | 否 | 每页条数，默认 20，最大 100 |
| type | string | 否 | `DAILY` / `LIMITED` |
| status | string | 否 | `ACTIVE` / `INACTIVE` |
| keyword | string | 否 | 名称模糊匹配 |

响应项：`id, name, type, price, stock, ruleConfig, status, createdAt`；`ruleConfig` 为 JSON 字符串，原样返回。

### 创建模板

`POST /api/v1/admin/blind-box/templates`

```json
{
  "name": "夏日限定盲盒",
  "type": "LIMITED",
  "price": 128.00,
  "stock": 100,
  "status": "ACTIVE",
  "ruleConfig": "{\"activityStartTime\":\"2026-07-01T00:00:00\",\"activityEndTime\":\"2026-07-07T23:59:59\",\"description\":\"夏日海岛\",\"discount\":8.5}"
}
```

- `DAILY`：`stock` 强制写为 `-1`（不限量），忽略传入库存；
- `LIMITED`：`stock >= 1`，`ruleConfig` 必填且包含 `activityStartTime` / `activityEndTime`（ISO `LocalDateTime`，开始早于结束）；
- 名称为空、价格 <= 0、限定库存不足或规则 JSON 非法 → **400**。

### 更新模板

`PUT /api/v1/admin/blind-box/templates/{id}`

- 仅校验本次提交的字段；
- `DAILY` 仍强制 `stock = -1`；
- 限量模板 `stock >= 0`，因此售罄（`stock = 0`）后仍可改名/下架；
- 修改类型为 `LIMITED` 或提交 `ruleConfig` 时校验活动窗口。

### 删除模板

`DELETE /api/v1/admin/blind-box/templates/{id}`

- 模板不存在 → **404**；
- 存在任意关联订单 → **400**（`存在关联订单，无法删除该模板`），应改为下架。

## 12. 盲盒订单管理（ADMIN）

### 订单列表

`GET /api/v1/admin/blind-box/orders?page&size&orderNo&userId&status&startDate&endDate`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 10，最大 100 |
| orderNo | string | 否 | 订单号模糊匹配 |
| userId | long | 否 | 用户 ID |
| status | string | 否 | `PENDING` / `PAID` / `PROCESSING` / `OPENED` / `REFUNDED` / `CANCELLED` |
| startDate | date | 否 | 创建日期起（`yyyy-MM-dd`） |
| endDate | date | 否 | 创建日期止（`yyyy-MM-dd`） |

响应项：`id, orderNo, userId, userName, templateId, templateName, payAmount, status, payMethod, payTime, expireAt, createdAt, refundReason, refundedAt`。用户名/模板名按订单批量查询填充，缺失时为 `null`。

### 订单详情

`GET /api/v1/admin/blind-box/orders/{orderNo}`

```json
{
  "order": { "orderNo": "BB202611210001", "status": "OPENED" },
  "result": { "destination": "杭州", "theme": "古镇", "resultText": "{...}" }
}
```

`result` 在未开盒时为 `null`；`result.resultText` 为完整方案 JSON 字符串。

### 退款

`POST /api/v1/admin/blind-box/orders/{orderNo}/refund`

```json
{ "reason": "用户申请退款" }
```

- `reason` 必填，最长 500；
- 仅 `PAID` / `OPENED` 可退款，否则 **400**；
- 使用 `WHERE id = ? AND status IN (PAID, OPENED)` 条件更新，置为 `REFUNDED` 并写入 `refundReason/refundedAt`；
- 并发退款导致影响行数为 0 → **400**，不会重复退款；
- 同一退款事务内恢复限定盲盒库存（`stock >= 0` 才 +1，日常盲盒不受影响），库存不依赖 MQ；
- 事务提交后发送 `ORDER_REFUNDED` 供下游通知/统计；发布失败仅记录日志，不影响已提交的退款与库存。

### 异常重试开盒

`POST /api/v1/admin/blind-box/orders/{orderNo}/retry-open`

- 仅 `PROCESSING` 可重试，否则 **400**；
- 事务提交后重新发送 `BLINDBOX_OPEN_REQUESTED`，由异步消费者重新生成方案。

## 13. P1.3 错误码补充

| HTTP | 场景 |
|------|------|
| 400 | 模板校验失败（名称/价格/库存/活动窗口）、有订单不可删、非 PAID/OPENED 退款、重复退款、非 PROCESSING 重试 |
| 403 | 普通用户/内容运营访问模板或订单管理 |
| 404 | 模板或订单不存在 |

## 14. P1.3 E2E

```bash
python -I ctrip-backend/scripts/e2e_admin_p13.py
```

覆盖：DAILY 库存 -1；LIMITED 窗口校验；非法模板 400；窗口内下单库存 5→4；窗口外下单 400；订单列表/详情；退款后库存恢复 5（仅一次）；重复退款/删除有订单模板 400；普通用户 403。

## 15. 轮播图管理（ADMIN）

### 管理端

`GET /api/v1/admin/banners?page&size&status`

响应项：`id, title, imageUrl, linkUrl, sortOrder, status, startAt, endAt, createdAt`。
排序：`sortOrder` 升序、`id` 升序；`size` 限制 1..100。

`POST /api/v1/admin/banners`

```json
{
  "title": "夏日旅行季",
  "imageUrl": "https://img.example.com/banner.jpg",
  "linkUrl": "/destinations",
  "sortOrder": 0,
  "status": "ENABLED",
  "startAt": "2026-11-21T00:00:00",
  "endAt": "2026-11-30T23:59:59"
}
```

- `title` / `imageUrl` 必填；`status` 默认 `ENABLED`，`sortOrder` 默认 0；
- `startAt/endAt` 均可为空；两者都非空时必须 `startAt < endAt`，否则 **400**。
- `PUT /{id}` 全字段更新；`DELETE /{id}` 不存在 → **404**。

### 公开端

`GET /api/v1/banners`（免登录）

仅返回 `status=ENABLED` 且当前时间处于 `[startAt, endAt]`（空表示不限）的记录，按 `sortOrder` 升序；无数据返回空数组，首页据此隐藏轮播区。

## 16. 推荐位管理（ADMIN）

### 管理端

`GET /api/v1/admin/recommendations?page&size&slot&status`

响应项：`id, slot, title, targetType, targetId, sortOrder, status, startAt, endAt, createdAt`。
`targetType`：`GUIDE` / `DESTINATION` / `BLIND_BOX_TEMPLATE`。

`POST /api/v1/admin/recommendations`

```json
{
  "slot": "home_featured",
  "title": "杭州精选",
  "targetType": "DESTINATION",
  "targetId": 1,
  "sortOrder": 0,
  "status": "ENABLED",
  "startAt": null,
  "endAt": null
}
```

- 字段必填；目标必须存在，否则 **400**（`推荐目标不存在`）；
- 时间窗规则同轮播图；`PUT /{id}`、`DELETE /{id}` 同模板 CRUD 约定。

### 公开端

`GET /api/v1/recommendations?slot=home_featured`（免登录）

仅返回启用且窗口内的记录，按 `sortOrder` 升序，并装配：

| 字段 | 说明 |
|------|------|
| targetTitle | 攻略标题 / 目的地名称 / 盲盒模板名称 |
| coverUrl | 攻略或目的地封面；盲盒模板为 null |
| linkUrl | `/guides/{id}` / `/destinations/{id}` / `/blind-box/{id}` |

攻略必须 `PUBLISHED`、盲盒模板必须 `ACTIVE`，目标缺失/未发布/已下架时跳过，不产生死链。

## 17. 平台数据概览（ADMIN）

`GET /api/v1/admin/dashboard`

```json
{
  "totalUsers": 100,
  "todayNewUsers": 3,
  "totalGuides": 40,
  "pendingGuides": 2,
  "publishedGuides": 30,
  "totalOrders": 50,
  "pendingOrders": 1,
  "paidOrders": 5,
  "processingOrders": 2,
  "openedOrders": 30,
  "refundedOrders": 3,
  "cancelledOrders": 9,
  "totalSales": 2680.00,
  "last7Days": [
    { "date": "2026-11-15", "newUsers": 1, "newOrders": 2 }
  ],
  "bannerCount": 3,
  "recommendationCount": 5
}
```

- `last7Days` 固定 7 项，缺失日期补 0；
- `totalSales` 仅统计 `PAID / PROCESSING / OPENED` 的 `payAmount`，退款/取消不计。

## 18. P1.4 E2E

```bash
python -I ctrip-backend/scripts/e2e_admin_p14.py
```

覆盖：Banner CRUD 与时间窗/启停过滤；推荐位目标装配、未发布攻略跳过；Dashboard 字段与 7 天序列；运营/普通用户 403；公开接口免登录。
