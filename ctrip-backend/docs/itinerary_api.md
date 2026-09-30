# 捷程旅行网 行程模块 API 接口文档

> 行程模块提供完整的行程规划能力，包含三级嵌套结构：**行程 → 日程 → 行程项**，以及收藏和分享功能。
> 所有接口均返回统一的 JSON 响应格式，基础路径为 `/api/v1`。

---

## 目录

- [通用说明](#通用说明)
  - [响应格式](#响应格式)
  - [认证方式](#认证方式)
  - [错误码说明](#错误码说明)
- [行程接口](#行程接口)
  - [创建行程](#创建行程)
  - [我的行程列表](#我的行程列表)
  - [行程详情](#行程详情)
  - [编辑行程](#编辑行程)
  - [删除行程](#删除行程)
- [日程接口](#日程接口)
  - [添加日程](#添加日程)
  - [编辑日程](#编辑日程)
  - [删除日程](#删除日程)
- [行程项接口](#行程项接口)
  - [添加行程项](#添加行程项)
  - [编辑行程项](#编辑行程项)
  - [删除行程项](#删除行程项)
  - [批量排序行程项](#批量排序行程项)
- [收藏接口](#收藏接口)
  - [收藏行程](#收藏行程)
  - [取消收藏](#取消收藏)
- [分享接口](#分享接口)
  - [生成分享链接](#生成分享链接)
  - [查看分享行程](#查看分享行程)
  - [取消分享](#取消分享)
- [附录](#附录)
  - [行程状态枚举](#行程状态枚举)
  - [行程项类型枚举](#行程项类型枚举)
  - [收藏目标类型枚举](#收藏目标类型枚举)
  - [API 端点汇总](#api-端点汇总)

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

- **分享查看接口**（`GET /api/v1/share/{shareCode}`）：无需认证
- **其他所有行程接口**：必须在请求头中携带有效的 Access Token

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
| 400 | Bad Request | 请求参数校验失败（如日期格式错误、必填字段缺失） |
| 401 | Unauthorized | 未登录或 Token 无效/过期 |
| 403 | Forbidden | 权限不足（如尝试编辑他人行程、查看草稿行程） |
| 404 | Not Found | 资源不存在（行程/日程/行程项） |
| 409 | Conflict | 资源冲突（如分享短码冲突） |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 行程接口

> 以下接口均需在请求头中携带有效的 Access Token。

### 创建行程

创建新行程，系统根据起止日期自动生成对应的日程框架（每天一条日程记录）。初始状态为 `DRAFT`（草稿）。

- **URL**: `POST /api/v1/itineraries`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "title": "上海三日深度游",
  "destinationId": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-03"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 行程标题，1-200 位 |
| destinationId | number | 否 | 关联的目的地 ID |
| startDate | string | 是 | 开始日期，格式 `YYYY-MM-DD` |
| endDate | string | 是 | 结束日期，格式 `YYYY-MM-DD`，不能早于 startDate |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "上海三日深度游",
    "destinationId": 1,
    "startDate": "2026-06-01",
    "endDate": "2026-06-03",
    "status": "DRAFT",
    "shareCode": null,
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": "2026-05-23T10:00:00",
    "updatedAt": "2026-05-23T10:00:00",
    "days": [
      {
        "id": 1,
        "dayNumber": 1,
        "title": "Day 1",
        "sortOrder": 1,
        "items": []
      },
      {
        "id": 2,
        "dayNumber": 2,
        "title": "Day 2",
        "sortOrder": 2,
        "items": []
      },
      {
        "id": 3,
        "dayNumber": 3,
        "title": "Day 3",
        "sortOrder": 3,
        "items": []
      }
    ]
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
  "error": "行程标题不能为空; 开始日期不能为空"
}

// 日期非法 (400)
{
  "success": false,
  "data": null,
  "error": "开始日期不能晚于结束日期"
}

// 未登录 (401)
{
  "success": false,
  "data": null,
  "error": "请先登录"
}
```

---

### 我的行程列表

查询当前用户的行程列表（分页），仅返回行程概要信息，不展开日程明细。

- **URL**: `GET /api/v1/itineraries`
- **认证**: 需要 JWT Token

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | number | 否 | 1 | 页码，从 1 开始 |
| limit | number | 否 | 10 | 每页条数 |
| status | string | 否 | - | 按状态筛选：`DRAFT` / `PUBLISHED` / `ARCHIVED` |
| destinationId | number | 否 | - | 按目的地 ID 筛选 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/itineraries?page=1&limit=10&status=DRAFT" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "上海三日深度游",
        "destinationId": 1,
        "startDate": "2026-06-01",
        "endDate": "2026-06-03",
        "status": "DRAFT",
        "viewCount": 0,
        "createdAt": "2026-05-23T10:00:00",
        "updatedAt": "2026-05-23T10:00:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10,
    "pages": 1
  },
  "error": null
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| records | array | 行程列表（每行仅含概要） |
| total | number | 总记录数 |
| current | number | 当前页码 |
| size | number | 每页条数 |
| pages | number | 总页数 |

---

### 行程详情

获取指定行程的完整信息，包含所有日程及行程项的嵌套结构。

- **URL**: `GET /api/v1/itineraries/{id}`
- **认证**: 需要 JWT Token

#### 权限说明

| 行程状态 | 创建者 | 其他登录用户 |
|----------|--------|------------|
| DRAFT | 可查看完整详情 | 403 无权查看 |
| PUBLISHED | 可查看完整详情 | 可查看（非创建者访问时 viewCount +1） |
| ARCHIVED | 可查看完整详情 | 可查看（只读） |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "上海三日深度游",
    "destinationId": 1,
    "startDate": "2026-06-01",
    "endDate": "2026-06-03",
    "status": "PUBLISHED",
    "shareCode": "aB3xK9",
    "viewCount": 128,
    "likeCount": 5,
    "createdAt": "2026-05-23T10:00:00",
    "updatedAt": "2026-05-23T10:00:00",
    "days": [
      {
        "id": 1,
        "dayNumber": 1,
        "title": "Day 1：抵达上海",
        "sortOrder": 1,
        "items": [
          {
            "id": 1,
            "itineraryDayId": 1,
            "type": "TRANSPORT",
            "name": "高铁抵达上海虹桥站",
            "location": "上海虹桥火车站",
            "timeSlot": "09:00-12:00",
            "description": "乘坐 G2 次列车",
            "sortOrder": 1
          },
          {
            "id": 2,
            "itineraryDayId": 1,
            "type": "HOTEL",
            "name": "全季酒店（外滩店）",
            "location": "上海市黄浦区南京东路 100 号",
            "timeSlot": "14:00-15:00",
            "description": "办理入住",
            "sortOrder": 2
          },
          {
            "id": 3,
            "itineraryDayId": 1,
            "type": "ATTRACTION",
            "name": "外滩",
            "location": "上海市黄浦区中山东一路",
            "timeSlot": "16:00-19:00",
            "description": "欣赏黄浦江夜景",
            "sortOrder": 3
          }
        ]
      }
    ]
  },
  "error": null
}
```

#### 失败响应

```json
// 行程不存在 (404)
{
  "success": false,
  "data": null,
  "error": "行程不存在：id=999"
}

// 无权查看他人草稿 (403)
{
  "success": false,
  "data": null,
  "error": "无权查看该行程"
}
```

---

### 编辑行程

更新行程的基本信息。支持部分更新（仅传入需要修改的字段）。

- **URL**: `PUT /api/v1/itineraries/{id}`
- **认证**: 需要 JWT Token

#### 请求参数

所有字段均为可选，仅传入需要修改的字段。

```json
{
  "title": "上海四日精华游",
  "destinationId": 2,
  "startDate": "2026-07-01",
  "endDate": "2026-07-04",
  "status": "PUBLISHED"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 行程标题，1-200 位 |
| destinationId | number | 否 | 关联的目的地 ID |
| startDate | string | 否 | 开始日期，格式 `YYYY-MM-DD` |
| endDate | string | 否 | 结束日期，格式 `YYYY-MM-DD` |
| status | string | 否 | 行程状态：`DRAFT` / `PUBLISHED` / `ARCHIVED` |

#### 成功响应 (200)

返回更新后的完整行程详情（格式同 [行程详情](#行程详情)）。

#### 失败响应

```json
// 已归档不可修改 (403)
{
  "success": false,
  "data": null,
  "error": "已归档的行程不可修改"
}

// 无权操作他人行程 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作他人行程"
}

// 日期非法 (400)
{
  "success": false,
  "data": null,
  "error": "开始日期不能晚于结束日期"
}
```

---

### 删除行程

删除指定行程及其关联的所有日程和行程项（级联删除）。

- **URL**: `DELETE /api/v1/itineraries/{id}`
- **认证**: 需要 JWT Token

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

#### 失败响应

```json
// 无权删除他人行程 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作他人行程"
}

// 行程不存在 (404)
{
  "success": false,
  "data": null,
  "error": "行程不存在：id=999"
}
```

---

## 日程接口

> 日程（ItineraryDay）是行程下的子单元，每个行程包含若干天。

### 添加日程

向指定行程添加一个新的日程。

- **URL**: `POST /api/v1/itineraries/{id}/days`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "dayNumber": 4,
  "title": "Day 4：自由活动"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dayNumber | number | 是 | 第几天（从 1 开始） |
| title | string | 是 | 日程标题，不能为空 |

#### 成功响应 (200)

返回更新后的完整行程详情（含新增日程，格式同 [行程详情](#行程详情)）。

---

### 编辑日程

更新日程的标题或排序。

- **URL**: `PUT /api/v1/itineraries/days/{dayId}`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "title": "Day 1：抵达与安顿",
  "sortOrder": 2
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 日程标题 |
| sortOrder | number | 否 | 排序序号 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "dayNumber": 1,
    "title": "Day 1：抵达与安顿",
    "sortOrder": 2,
    "items": []
  },
  "error": null
}
```

---

### 删除日程

删除指定日程及其关联的所有行程项（级联删除）。

- **URL**: `DELETE /api/v1/itineraries/days/{dayId}`
- **认证**: 需要 JWT Token

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 行程项接口

> 行程项（ItineraryItem）是日程下的具体活动，如酒店、景点、交通等。

### 添加行程项

向指定日程添加一个行程项。

- **URL**: `POST /api/v1/itineraries/{id}/days/{dayId}/items`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "type": "ATTRACTION",
  "name": "东方明珠塔",
  "location": "上海市浦东新区世纪大道 1 号",
  "timeSlot": "10:00-12:00",
  "description": "登塔观景，含透明观光层",
  "sortOrder": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 类型：`HOTEL` / `ATTRACTION` / `TRANSPORT` / `FOOD` / `ACTIVITY` |
| name | string | 是 | 名称，不能为空 |
| location | string | 否 | 位置/地址 |
| timeSlot | string | 否 | 时间段，如 `09:00-11:00` |
| description | string | 否 | 详细描述 |
| sortOrder | number | 否 | 排序序号（不传则自动追加到末尾） |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 4,
    "itineraryDayId": 1,
    "type": "ATTRACTION",
    "name": "东方明珠塔",
    "location": "上海市浦东新区世纪大道 1 号",
    "timeSlot": "10:00-12:00",
    "description": "登塔观景，含透明观光层",
    "sortOrder": 1
  },
  "error": null
}
```

---

### 编辑行程项

更新行程项的信息。支持部分更新。

- **URL**: `PUT /api/v1/itineraries/items/{itemId}`
- **认证**: 需要 JWT Token

#### 请求参数

所有字段均为可选。

```json
{
  "name": "东方明珠广播电视塔",
  "timeSlot": "09:00-11:30",
  "description": "登塔观景，含透明观光层和旋转餐厅"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 类型枚举值 |
| name | string | 否 | 名称 |
| location | string | 否 | 位置/地址 |
| timeSlot | string | 否 | 时间段 |
| description | string | 否 | 详细描述 |
| sortOrder | number | 否 | 排序序号 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 4,
    "itineraryDayId": 1,
    "type": "ATTRACTION",
    "name": "东方明珠广播电视塔",
    "location": "上海市浦东新区世纪大道 1 号",
    "timeSlot": "09:00-11:30",
    "description": "登塔观景，含透明观光层和旋转餐厅",
    "sortOrder": 1
  },
  "error": null
}
```

---

### 删除行程项

删除指定的行程项。

- **URL**: `DELETE /api/v1/itineraries/items/{itemId}`
- **认证**: 需要 JWT Token

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 批量排序行程项

对指定日程下的所有行程项进行批量排序。传入有序的 item ID 列表，系统按列表顺序重新分配 sortOrder。

- **URL**: `POST /api/v1/itineraries/days/{dayId}/items/reorder`
- **认证**: 需要 JWT Token

==reorder 接口需要传行程项的真实 ID（主键），不是序号或其他值。 前端调用时应从当前显示的 item 列表中取 ID==

#### 请求参数

```json
{
  "itemIds": [3, 1, 2, 4]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| itemIds | array\<number\> | 是 | 有序的行程项 ID 列表，所有 ID 必须属于该日程 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 收藏接口

### 收藏行程

将行程加入收藏。该接口为幂等操作，重复收藏不会产生重复记录。

- **URL**: `POST /api/v1/itineraries/{id}/collection?add=true`
- **认证**: 需要 JWT Token

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| add | boolean | 是 | `true` = 收藏，`false` = 取消收藏（Query 参数） |

#### 请求示例

```bash
# 收藏行程
curl -X POST "http://localhost:8080/api/v1/itineraries/1/collection?add=true" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# 取消收藏
curl -X POST "http://localhost:8080/api/v1/itineraries/1/collection?add=false" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 取消收藏

取消指定行程的收藏。该接口为幂等操作，不存在的收藏记录会静默处理。

- **URL**: `POST /api/v1/itineraries/{id}/collection?add=false`
- **认证**: 需要 JWT Token

（请求和响应格式同 [收藏行程](#收藏行程)）

---

## 分享接口

### 生成分享链接

为指定行程生成一个分享短码。分享链接有效期 7 天，过期后自动失效。

- **URL**: `POST /api/v1/itineraries/{id}/share`
- **认证**: 需要 JWT Token

#### 成功响应 (200)

```json
{
  "success": true,
  "data": "aB3xK9",
  "error": null
}
```

返回的短码对应分享链接：`/api/v1/share/aB3xK9`

#### 失败响应

```json
// 无权操作他人行程 (403)
{
  "success": false,
  "data": null,
  "error": "无权操作他人行程"
}
```

---

### 查看分享行程

通过分享短码查看行程详情。该接口为**公开接口**，无需登录。

- **URL**: `GET /api/v1/itineraries/share/{shareCode}`
- **认证**: 无需

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "上海三日深度游",
    "destinationId": 1,
    "startDate": "2026-06-01",
    "endDate": "2026-06-03",
    "status": "PUBLISHED",
    "shareCode": "aB3xK9",
    "viewCount": 129,
    "likeCount": 5,
    "createdAt": "2026-05-23T10:00:00",
    "updatedAt": "2026-05-23T10:00:00",
    "days": [
      {
        "id": 1,
        "dayNumber": 1,
        "title": "Day 1：抵达上海",
        "sortOrder": 1,
        "items": [
          {
            "id": 1,
            "itineraryDayId": 1,
            "type": "TRANSPORT",
            "name": "高铁抵达上海虹桥站",
            "location": "上海虹桥火车站",
            "timeSlot": "09:00-12:00",
            "description": "乘坐 G2 次列车",
            "sortOrder": 1
          }
        ]
      }
    ]
  },
  "error": null
}
```

**注意**：分享响应中不暴露创建者的 `userId` 等敏感信息。

#### 失败响应

```json
// 分享链接无效 (404)
{
  "success": false,
  "data": null,
  "error": "分享链接无效"
}

// 分享链接已过期 (403)
{
  "success": false,
  "data": null,
  "error": "分享链接已过期"
}
```

---

### 取消分享

取消指定行程的分享链接。取消后，之前的分享短码立即失效。

- **URL**: `DELETE /api/v1/itineraries/{id}/share`
- **认证**: 需要 JWT Token

#### 成功响应 (200)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 附录

### 行程状态枚举

| 值 | 说明 |
|----|------|
| DRAFT | 草稿，仅创建者可见和编辑 |
| PUBLISHED | 已发布，他人可查看 |
| ARCHIVED | 已归档，只读不可编辑 |

### 行程项类型枚举

| 值 | 说明 |
|----|------|
| HOTEL | 酒店住宿 |
| ATTRACTION | 景点游览 |
| TRANSPORT | 交通出行 |
| FOOD | 餐饮美食 |
| ACTIVITY | 活动体验 |

### 收藏目标类型枚举

| 值 | 说明 |
|----|------|
| ITINERARY | 行程 |
| GUIDE | 攻略 |
| DESTINATION | 目的地 |

### API 端点汇总

#### 行程管理（需要 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries` | 创建行程 |
| GET | `/api/v1/itineraries` | 我的行程列表（分页） |
| GET | `/api/v1/itineraries/{id}` | 行程详情 |
| PUT | `/api/v1/itineraries/{id}` | 编辑行程 |
| DELETE | `/api/v1/itineraries/{id}` | 删除行程 |

#### 日程管理（需要 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries/{id}/days` | 添加日程 |
| PUT | `/api/v1/itineraries/days/{dayId}` | 编辑日程 |
| DELETE | `/api/v1/itineraries/days/{dayId}` | 删除日程（级联行程项） |

#### 行程项管理（需要 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries/{id}/days/{dayId}/items` | 添加行程项 |
| PUT | `/api/v1/itineraries/items/{itemId}` | 编辑行程项 |
| DELETE | `/api/v1/itineraries/items/{itemId}` | 删除行程项 |
| POST | `/api/v1/itineraries/days/{dayId}/items/reorder` | 批量排序行程项 |

#### 收藏功能（需要 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/itineraries/{id}/collection?add=true` | 收藏行程 |
| POST | `/api/v1/itineraries/{id}/collection?add=false` | 取消收藏 |

#### 分享功能

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/itineraries/{id}/share` | JWT | 生成分享链接 |
| GET | `/api/v1/share/{shareCode}` | 无需 | 查看分享行程（只读公开） |
| DELETE | `/api/v1/itineraries/{id}/share` | JWT | 取消分享 |
