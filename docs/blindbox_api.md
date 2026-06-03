# 盲盒模块 API 接口文档

> 所有接口均返回统一的 JSON 响应格式，基础路径为 `/api/v1`。
> 本模块遵循 `docs/other/api_standards.md` 规范。

---

## 目录

- [通用说明](#通用说明)
  - [响应格式](#响应格式)
  - [认证方式](#认证方式)
  - [错误码说明](#错误码说明)
- [盲盒模板接口](#盲盒模板接口)
  - [盲盒模板列表](#盲盒模板列表)
  - [模板详情](#模板详情)
  - [我的盲盒列表](#我的盲盒列表)
- [订单接口](#订单接口)
  - [创建订单](#创建订单)
  - [我的订单列表](#我的订单列表)
  - [订单详情](#订单详情)
  - [取消订单](#取消订单)
- [支付接口](#支付接口)
  - [模拟支付回调](#模拟支付回调)
- [开盒接口](#开盒接口)
  - [开盒](#开盒)
- [盲盒结果接口](#盲盒结果接口)
  - [结果详情](#结果详情)
- [分享接口](#分享接口)
  - [生成分享链接](#生成分享链接)
  - [查看分享结果](#查看分享结果)
- [后台管理接口](#后台管理接口)
  - [模板列表](#模板列表)
  - [创建模板](#创建模板)
  - [更新模板](#更新模板)
  - [删除模板](#删除模板)
  - [全部订单列表](#全部订单列表)
- [附录](#附录)
  - [盲盒类型枚举](#盲盒类型枚举)
  - [模板状态枚举](#模板状态枚举)
  - [订单状态枚举](#订单状态枚举)
  - [预算等级枚举](#预算等级枚举)
  - [ruleConfig 结构说明](#ruleconfig-结构说明)
  - [resultText 结构说明](#resulttext-结构说明)

---

## 通用说明

### 响应格式

所有接口均返回统一的 JSON 响应信封：

```json
// 成功响应
{
  "success": true,
  "data": { /* 业务数据 */ },
  "error": null
}

// 失败响应
{
  "success": false,
  "data": null,
  "error": "错误描述信息"
}
```

### 认证方式

- **公开接口**（盲盒列表、模板详情、支付回调、分享查看）：无需携带 JWT Token
- **用户接口**（订单、开盒、结果、分享生成）：必须在请求头中携带有效的 Access Token
- **管理接口**（模板 CRUD、订单管理）：需要 JWT Token + ADMIN 角色

```
Authorization: Bearer <access_token>
```

| Token 类型 | 有效期 | 说明 |
|-----------|-------|------|
| Access Token | 15 分钟 | 用于接口鉴权，过期后需刷新 |
| Refresh Token | 15 天 | 用于刷新 Access Token，存储于客户端 |

### 错误码说明

| HTTP 状态码 | 含义 | 说明 |
|------------|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功（创建订单等） |
| 400 | Bad Request | 请求参数校验失败 / 业务异常（库存不足、订单已过期等） |
| 401 | Unauthorized | 未登录或 Token 无效/过期 |
| 403 | Forbidden | 权限不足 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（重复开盒、分享码冲突） |
| 429 | Too Many Requests | 请求频率超限 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 盲盒模板接口

### 盲盒模板列表

查询所有上架中的盲盒模板，支持分页和类型筛选。

- **URL**: `GET /api/v1/blind-box`
- **认证**: 无需

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| type | String | 否 | 筛选盲盒类型：`DAILY` / `LIMITED` |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box?page=1&limit=20&type=DAILY"
```

#### 成功响应 (200)

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
        "ruleConfig": {
          "description": "日常随机旅行体验"
        },
        "status": "ACTIVE",
        "createdAt": "2026-06-01T10:00:00"
      },
      {
        "id": 2,
        "name": "夏日海岛限定盲盒",
        "type": "LIMITED",
        "price": 199.00,
        "stock": 50,
        "ruleConfig": {
          "activityStartTime": "2026-07-01T00:00:00",
          "activityEndTime": "2026-07-07T23:59:59",
          "discount": 0.8,
          "description": "夏日限定海岛盲盒"
        },
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

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 模板 ID |
| name | string | 盲盒名称 |
| type | string | 盲盒类型：`DAILY` / `LIMITED` |
| price | number | 价格（单位：元） |
| stock | number | 库存数量（`-1` 表示不限量） |
| ruleConfig | object | 规则配置（见 [ruleConfig 结构说明](#ruleconfig-结构说明)） |
| status | string | 模板状态：`ACTIVE` / `INACTIVE` |
| createdAt | string | 创建时间，ISO 8601 格式 |

#### 失败响应

```json
// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "页码必须大于 0"
}
```

---

### 模板详情

根据 ID 查询单个盲盒模板的详细信息。

- **URL**: `GET /api/v1/blind-box/{id}`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模板 ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/1"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "日常旅行盲盒",
    "type": "DAILY",
    "price": 99.00,
    "stock": -1,
    "ruleConfig": {
      "description": "日常随机旅行体验"
    },
    "status": "ACTIVE",
    "createdAt": "2026-06-01T10:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 模板不存在 (404)
{
  "success": false,
  "data": null,
  "error": "盲盒模板不存在"
}

// 模板已下架 (400)
{
  "success": false,
  "data": null,
  "error": "该盲盒模板已下架"
}
```

---

### 我的盲盒列表

查询当前用户的盲盒订单及对应的开盒结果，支持分页和开盒状态筛选。

- **URL**: `GET /api/v1/blind-box/my`
- **认证**: 需要 JWT Token

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| opened | Boolean | 否 | 筛选：`true`=已开盒，`false`=未开盒 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/my?page=1&limit=20&opened=true" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "orderNo": "BB202606010005",
        "templateName": "夏日海岛限定盲盒",
        "status": "OPENED",
        "payAmount": 199.00,
        "result": {
          "destination": "三亚",
          "theme": "海滨",
          "budgetLevel": "STANDARD",
          "days": 5,
          "shareCode": "xK9mPq",
          "openedAt": "2026-06-01T16:00:00"
        },
        "createdAt": "2026-06-01T15:30:00"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20,
    "pages": 1
  },
  "error": null
}
```

---

## 订单接口

> 以下接口均需在请求头中携带有效的 Access Token：
> ```
> Authorization: Bearer <access_token>
> ```

### 创建订单

创建盲盒购买订单，同时提交预选参数（出发地、预算、主题）。

- **URL**: `POST /api/v1/blind-box/orders`
- **认证**: 需要 JWT Token
- **说明**: 订单创建后状态为 `PENDING`，需在 15 分钟内完成支付，否则自动取消

#### 请求参数

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
| templateId | Long | 是 | 盲盒模板 ID |
| departureCity | String | 是 | 出发城市 |
| budgetLevel | String | 是 | 预算等级：`ECONOMY` / `STANDARD` / `LUXURY` |
| theme | String | 否 | 旅行主题（可选） |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/blind-box/orders" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": 1,
    "departureCity": "上海",
    "budgetLevel": "STANDARD",
    "theme": "美食"
  }'
```

#### 成功响应 (201)

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

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| orderNo | string | 订单编号（业务主键，格式 `BByyyyMMddXXXX`） |
| templateId | number | 盲盒模板 ID |
| templateName | string | 盲盒模板名称 |
| payAmount | number | 实际支付金额（单位：元） |
| expireAt | string | 支付超时时间（创建后 15 分钟），ISO 8601 格式 |
| status | string | 订单状态：`PENDING` / `PAID` / `OPENED` / `REFUNDED` / `CANCELLED` |

#### 失败响应

```json
// 模板不存在或已下架 (400)
{
  "success": false,
  "data": null,
  "error": "盲盒模板不存在或已下架"
}

// 限定盲盒库存不足 (400)
{
  "success": false,
  "data": null,
  "error": "限定盲盒库存不足"
}

// 限定盲盒活动未开始或已结束 (400)
{
  "success": false,
  "data": null,
  "error": "该限定盲盒活动尚未开始 / 已结束"
}

// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "预算等级必须为 ECONOMY、STANDARD 或 LUXURY"
}
```

---

### 我的订单列表

查询当前用户的盲盒订单列表，支持分页和状态筛选。

- **URL**: `GET /api/v1/blind-box/orders`
- **认证**: 需要 JWT Token

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| status | String | 否 | 筛选订单状态 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/orders?page=1&limit=20&status=PENDING" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "orderNo": "BB202606020001",
        "templateId": 1,
        "templateName": "日常旅行盲盒",
        "payAmount": 99.00,
        "status": "PENDING",
        "expireAt": "2026-06-02T10:45:00",
        "payTime": null,
        "createdAt": "2026-06-02T10:30:00"
      },
      {
        "orderNo": "BB202606010005",
        "templateId": 2,
        "templateName": "夏日海岛限定盲盒",
        "payAmount": 199.00,
        "status": "OPENED",
        "expireAt": "2026-06-01T15:45:00",
        "payTime": "2026-06-01T15:30:00",
        "createdAt": "2026-06-01T15:30:00"
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

---

### 订单详情

根据订单编号查询单个订单的详细信息。

- **URL**: `GET /api/v1/blind-box/orders/{orderNo}`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/orders/BB202606020001" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "templateId": 1,
    "templateName": "日常旅行盲盒",
    "payAmount": 99.00,
    "payMethod": null,
    "payTime": null,
    "status": "PENDING",
    "expireAt": "2026-06-02T10:45:00",
    "createdAt": "2026-06-02T10:30:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单不属于当前用户 (403)
{
  "success": false,
  "data": null,
  "error": "无权访问该订单"
}
```

---

### 取消订单

取消待支付的订单。仅状态为 `PENDING` 的订单可取消，取消后若为限定盲盒将恢复库存。

- **URL**: `POST /api/v1/blind-box/orders/{orderNo}/cancel`
- **认证**: 需要 JWT Token
- **说明**: 幂等操作，已取消的订单重复调用不会报错

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/blind-box/orders/BB202606020001/cancel" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "status": "CANCELLED",
    "message": "订单已取消"
  },
  "error": null
}
```

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单不属于当前用户 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作该订单"
}

// 订单状态不允许取消（仅 PENDING 可取消）(400)
{
  "success": false,
  "data": null,
  "error": "该订单不可取消"
}
```

---

## 支付接口

### 模拟支付回调

MVP 阶段使用模拟支付，不对接真实支付渠道。支付成功后订单状态变更为 `PAID`。

- **URL**: `POST /api/v1/payments/blind-box/callback`
- **认证**: 无需

#### 请求参数

```json
{
  "orderNo": "BB202606020001",
  "payMethod": "ALIPAY"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |
| payMethod | String | 是 | 支付方式：`ALIPAY` / `WECHAT_PAY` |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/payments/blind-box/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "BB202606020001",
    "payMethod": "ALIPAY"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "status": "PAID",
    "payTime": "2026-06-02T10:35:00",
    "payMethod": "ALIPAY",
    "message": "支付成功"
  },
  "error": null
}
```

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单已支付（幂等处理）(200)
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "status": "PAID",
    "message": "订单已支付"
  },
  "error": null
}

// 订单已过期/取消 (400)
{
  "success": false,
  "data": null,
  "error": "订单已过期，无法支付"
}
```

---

## 开盒接口

### 开盒

打开已支付的盲盒订单，生成旅行方案结果。

- **URL**: `POST /api/v1/blind-box/orders/{orderNo}/open`
- **认证**: 需要 JWT Token
- **说明**: 幂等操作，已开盒的订单重复调用返回同一结果

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/blind-box/orders/BB202606020001/open" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "resultId": 1,
    "destination": "成都",
    "destinationId": 5,
    "theme": "美食",
    "resultText": {
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
    },
    "shareCode": "aB3xYz",
    "openedAt": "2026-06-02T11:00:00"
  },
  "error": null
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| orderNo | string | 订单编号 |
| resultId | number | 盲盒结果 ID |
| destination | string | 目的地名称 |
| destinationId | number | 目的地 ID |
| theme | string | 旅行主题 |
| resultText | object | 完整旅行方案 JSON（见 [resultText 结构说明](#resulttext-结构说明)） |
| shareCode | string | 分享链接短码（6 位 Base62） |
| openedAt | string | 开盒时间，ISO 8601 格式 |

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单不属于当前用户 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作该订单"
}

// 订单未支付 (400)
{
  "success": false,
  "data": null,
  "error": "请先完成支付"
}

// 订单已开盒（幂等返回）(200)
{
  "success": true,
  "data": { /* 已有结果，同上 */ },
  "error": null
}
```

---

## 盲盒结果接口

> 以下接口均需在请求头中携带有效的 Access Token。

### 结果详情

查询指定订单的盲盒开盒结果详情。

- **URL**: `GET /api/v1/blind-box/orders/{orderNo}/result`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/orders/BB202606020001/result" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "resultId": 1,
    "orderNo": "BB202606020001",
    "destination": "成都",
    "destinationId": 5,
    "theme": "美食",
    "budgetLevel": "STANDARD",
    "days": 4,
    "resultText": {
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
    },
    "shareCode": "aB3xYz",
    "openedAt": "2026-06-02T11:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单未开盒 (400)
{
  "success": false,
  "data": null,
  "error": "该订单尚未开盒"
}

// 订单不属于当前用户 (403)
{
  "success": false,
  "data": null,
  "error": "无权访问该结果"
}
```

---

## 分享接口

### 生成分享链接

为已开盒的盲盒结果生成分享链接（6 位 Base62 短码）。已分享过的订单将复用原有短码。

- **URL**: `POST /api/v1/blind-box/orders/{orderNo}/share-result`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/blind-box/orders/BB202606020001/share-result" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "orderNo": "BB202606020001",
    "shareCode": "aB3xYz",
    "shareUrl": "http://localhost:8080/api/v1/blind-box/share/aB3xYz",
    "expiresAt": "2026-06-09T11:00:00"
  },
  "error": null
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| orderNo | string | 订单编号 |
| shareCode | string | 分享短码（6 位 Base62） |
| shareUrl | string | 分享链接完整 URL |
| expiresAt | string | 分享链接过期时间（默认 7 天后），ISO 8601 格式 |

#### 失败响应

```json
// 订单不存在 (404)
{
  "success": false,
  "data": null,
  "error": "订单不存在"
}

// 订单未开盒 (400)
{
  "success": false,
  "data": null,
  "error": "请先开盒后再生成分享链接"
}

// 订单不属于当前用户 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作该订单"
}
```

---

### 查看分享结果

通过分享短码查看他人的盲盒旅行方案概览（公开接口，无需登录）。

- **URL**: `GET /api/v1/blind-box/share/{shareCode}`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shareCode | String | 是 | 分享短码（6 位 Base62） |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/blind-box/share/aB3xYz"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
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
    "budget": { "total": 2999, "transport": 800, "hotel": 1200, "food": 600, "tickets": 399 },
    "openedAt": "2026-06-02T11:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 分享链接不存在 (404)
{
  "success": false,
  "data": null,
  "error": "分享链接不存在"
}

// 分享链接已过期 (400)
{
  "success": false,
  "data": null,
  "error": "分享链接已过期"
}
```

---

## 后台管理接口

> 以下接口均需在请求头中携带有效的 Access Token，且用户角色为 `ADMIN`。

### 模板列表

查询所有盲盒模板（含已下架），支持分页和类型筛选。

- **URL**: `GET /api/v1/admin/blind-box/templates`
- **认证**: 需要 JWT Token + ADMIN 角色

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| type | String | 否 | 筛选盲盒类型：`DAILY` / `LIMITED` |
| status | String | 否 | 筛选模板状态：`ACTIVE` / `INACTIVE` |

#### 成功响应 (200)

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
        "createdAt": "2026-06-01T10:00:00",
        "updatedAt": "2026-06-01T10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20,
    "pages": 1
  },
  "error": null
}
```

---

### 创建模板

创建新的盲盒模板。

- **URL**: `POST /api/v1/admin/blind-box/templates`
- **认证**: 需要 JWT Token + ADMIN 角色

#### 请求参数

```json
{
  "name": "日常旅行盲盒",
  "type": "DAILY",
  "price": 99.00,
  "ruleConfig": {
    "description": "日常随机旅行体验"
  }
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 盲盒名称，最长 100 位 |
| type | String | 是 | 盲盒类型：`DAILY` / `LIMITED` |
| price | BigDecimal | 是 | 价格（单位：元），必须 > 0 |
| stock | Integer | 否 | 库存数量（`DAILY` 类型自动设为 `-1`） |
| ruleConfig | Object | 否 | 规则配置（见 [ruleConfig 结构说明](#ruleconfig-结构说明)） |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/admin/blind-box/templates" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "日常旅行盲盒",
    "type": "DAILY",
    "price": 99.00,
    "ruleConfig": {
      "description": "日常随机旅行体验"
    }
  }'
```

#### 成功响应 (201)

```json
{
  "success": true,
  "data": {
    "id": 3,
    "name": "日常旅行盲盒",
    "type": "DAILY",
    "price": 99.00,
    "stock": -1,
    "ruleConfig": {
      "description": "日常随机旅行体验"
    },
    "status": "ACTIVE",
    "createdAt": "2026-06-03T10:00:00",
    "updatedAt": "2026-06-03T10:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "价格必须大于 0"
}

// 类型不匹配 (400)
{
  "success": false,
  "data": null,
  "error": "DAILY 类型盲盒库存必须为 -1（不限量）"
}
```

---

### 更新模板

更新指定盲盒模板的信息。

- **URL**: `PUT /api/v1/admin/blind-box/templates/{id}`
- **认证**: 需要 JWT Token + ADMIN 角色
- **说明**: 仅传入需要修改的字段

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模板 ID |

#### 请求参数

```json
{
  "name": "新盲盒名称",
  "price": 129.00,
  "stock": 100,
  "ruleConfig": {
    "activityStartTime": "2026-07-01T00:00:00",
    "activityEndTime": "2026-07-07T23:59:59",
    "discount": 0.8,
    "description": "夏日限定海岛盲盒"
  },
  "status": "INACTIVE"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 盲盒名称，最长 100 位 |
| price | BigDecimal | 否 | 价格（单位：元），必须 > 0 |
| stock | Integer | 否 | 库存数量 |
| ruleConfig | Object | 否 | 规则配置 |
| status | String | 否 | 模板状态：`ACTIVE` / `INACTIVE` |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "新盲盒名称",
    "type": "DAILY",
    "price": 129.00,
    "stock": -1,
    "ruleConfig": {
      "description": "日常随机旅行体验（已更新）"
    },
    "status": "ACTIVE",
    "createdAt": "2026-06-01T10:00:00",
    "updatedAt": "2026-06-03T12:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 模板不存在 (404)
{
  "success": false,
  "data": null,
  "error": "盲盒模板不存在"
}

// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "价格必须大于 0"
}
```

---

### 删除模板

删除指定的盲盒模板（软删除）。

- **URL**: `DELETE /api/v1/admin/blind-box/templates/{id}`
- **认证**: 需要 JWT Token + ADMIN 角色

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模板 ID |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "模板已删除"
  },
  "error": null
}
```

#### 失败响应

```json
// 模板不存在 (404)
{
  "success": false,
  "data": null,
  "error": "盲盒模板不存在"
}

// 模板下存在关联订单 (400)
{
  "success": false,
  "data": null,
  "error": "该模板下存在关联订单，无法删除"
}
```

---

### 全部订单列表

查询所有用户的盲盒订单，支持分页和状态筛选。

- **URL**: `GET /api/v1/admin/blind-box/orders`
- **认证**: 需要 JWT Token + ADMIN 角色

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 20 |
| status | String | 否 | 筛选订单状态 |
| templateId | Long | 否 | 筛选模板 ID |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "orderNo": "BB202606020001",
        "userId": 10,
        "username": "张三",
        "templateId": 1,
        "templateName": "日常旅行盲盒",
        "payAmount": 99.00,
        "status": "PENDING",
        "expireAt": "2026-06-02T10:45:00",
        "payTime": null,
        "createdAt": "2026-06-02T10:30:00"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20,
    "pages": 1
  },
  "error": null
}
```

---

## 附录

### 盲盒类型枚举

| 值 | 说明 |
|----|------|
| DAILY | 日常盲盒，不限量，随时购买 |
| LIMITED | 限定盲盒，限时限量抢购 |

### 模板状态枚举

| 值 | 说明 |
|----|------|
| ACTIVE | 上架中，用户可购买 |
| INACTIVE | 已下架 |

### 订单状态枚举

| 值 | 说明 |
|----|------|
| PENDING | 待支付（创建后 15 分钟超时） |
| PAID | 已支付，待开盒 |
| OPENED | 已开盒 |
| REFUNDED | 已退款 |
| CANCELLED | 已取消（超时未支付 / 用户主动取消） |

### 预算等级枚举

| 值 | 说明 |
|----|------|
| ECONOMY | 经济型（低价位酒店/交通） |
| STANDARD | 标准型（中等价位） |
| LUXURY | 豪华型（高端酒店/交通） |

### ruleConfig 结构说明

`ruleConfig` 为 JSON 对象，用于配置盲盒的规则信息。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| activityStartTime | String | LIMITED 类型必填 | 活动开始时间，ISO 8601 格式 |
| activityEndTime | String | LIMITED 类型必填 | 活动结束时间，ISO 8601 格式 |
| discount | Number | 否 | 折扣系数（如 `0.8` 表示 8 折） |
| description | String | 否 | 盲盒描述说明 |

示例：
```json
{
  "activityStartTime": "2026-07-01T00:00:00",
  "activityEndTime": "2026-07-07T23:59:59",
  "discount": 0.8,
  "description": "夏日限定海岛盲盒"
}
```

### resultText 结构说明

`resultText` 为 JSON 对象，包含盲盒开盒后生成的完整旅行方案。

| 字段 | 类型 | 说明 |
|------|------|------|
| destination | String | 目的地名称 |
| destinationId | Number | 目的地 ID |
| theme | String | 旅行主题 |
| budgetLevel | String | 预算等级 |
| days | Number | 行程天数 |
| itinerary | Object | 行程安排（按天分组） |
| hotel | Object | 酒店信息（名称、地址、评分） |
| transport | Object | 交通信息（类型、出发时间） |
| budget | Object | 预算明细（总计、交通、酒店、餐饮、门票） |

示例：
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

### 暂不实现功能

> 以下功能在 MVP 阶段暂不实现，保留设计文档，后续迭代接入：

| 功能 | 路径 | 说明 |
|------|------|------|
| AI 图片分析 | `POST /api/v1/blind-box/analyze-image` | AI 处理图片提取特征信息 |
| AI 结果图生成 | `result_image_url` 字段 | 盲盒结果图中的 AI 可视化行程单图片 |
| 结果图下载 | `GET /api/v1/blind-box/orders/{orderNo}/result-image` | 下载 AI 生成的结果图 |
| 图片标签存储 | `blind_box_preference.image_tags` | AI 图片分析结果存储字段 |
