# 捷程旅行网 Content 模块 API 接口文档

> 所有接口均返回统一的 JSON 响应格式，基础路径为 `/api/v1`。

---

## 目录

- [通用说明](#通用说明)
  - [响应格式](#响应格式)
  - [认证方式](#认证方式)
  - [分页格式](#分页格式)
  - [错误码说明](#错误码说明)
- [目的地接口](#目的地接口)
  - [目的地列表](#目的地列表)
  - [目的地详情](#目的地详情)
- [景点接口](#景点接口)
  - [景点列表](#景点列表)
  - [景点详情](#景点详情)
- [攻略接口](#攻略接口)
  - [攻略列表](#攻略列表)
  - [攻略详情](#攻略详情)
  - [发布攻略](#发布攻略)
  - [编辑攻略](#编辑攻略)
  - [删除攻略](#删除攻略)
  - [点赞攻略](#点赞攻略)
  - [取消点赞](#取消点赞)
- [评论接口](#评论接口)
  - [评论列表](#评论列表)
  - [发表评论](#发表评论)
  - [删除评论](#删除评论)
- [上传接口](#上传接口)
  - [上传图片](#上传图片)
  - [批量上传图片](#批量上传图片)
  - [删除图片](#删除图片)
- [管理端 — 目的地](#管理端--目的地)
  - [创建目的地](#创建目的地)
  - [更新目的地](#更新目的地)
  - [删除目的地](#删除目的地)
- [管理端 — 景点](#管理端--景点)
  - [创建景点](#创建景点)
  - [更新景点](#更新景点)
  - [删除景点](#删除景点)
- [附录](#附录)
  - [季节枚举](#季节枚举)
  - [攻略状态枚举](#攻略状态枚举)
  - [排序方式](#排序方式)

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

- **公开接口**（`/api/v1/destinations/**`、`/api/v1/attractions/**`、`/api/v1/guides/**` 的 GET 请求、`/api/v1/guides/{guideId}/comments` 的 GET 请求）：无需认证
- **用户接口**（攻略发布/编辑/删除/点赞、评论发表/删除、图片上传）：必须在请求头中携带有效的 Access Token
- **管理端接口**（`/api/v1/admin/**`）：需要 ADMIN 或 CONTENT_OPERATOR 角色

```
Authorization: Bearer <access_token>
```

### 分页格式

列表接口使用 MyBatis Plus 分页对象，返回结构如下：

```json
{
  "success": true,
  "data": {
    "records": [ /* 数据列表 */ ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  },
  "error": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| records | array | 当前页数据列表 |
| total | number | 总记录数 |
| size | number | 每页大小 |
| current | number | 当前页码 |
| pages | number | 总页数 |

### 错误码说明

| HTTP 状态码 | 含义 | 说明 |
|------------|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 400 | Bad Request | 请求参数校验失败 |
| 401 | Unauthorized | 未登录或 Token 无效/过期 |
| 403 | Forbidden | 权限不足（非作者或非管理员） |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 目的地接口

### 目的地列表

分页查询目的地列表，支持按国家、省份、关键词筛选。

- **URL**: `GET /api/v1/destinations`
- **认证**: 无需

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从 1 开始 |
| limit | int | 否 | 20 | 每页数量，最大 100 |
| country | string | 否 | — | 按国家筛选 |
| province | string | 否 | — | 按省份筛选 |
| keyword | string | 否 | — | 按名称关键词搜索 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/destinations?page=1&limit=10&country=中国"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "杭州",
        "country": "中国",
        "province": "浙江省",
        "description": "上有天堂，下有苏杭",
        "bestSeason": "SPRING",
        "coverUrl": "https://example.com/covers/hangzhou.jpg",
        "imageUrls": "[\"url1\",\"url2\"]",
        "attractions": [
          {
            "id": 101,
            "destinationId": 1,
            "name": "西湖",
            "description": "世界文化遗产",
            "location": "杭州市西湖区",
            "ticketPrice": 0.00,
            "coverUrl": "https://example.com/covers/west-lake.jpg",
            "imageUrls": "[\"url1\",\"url2\"]",
            "createdAt": "2024-01-15T10:00:00"
          }
        ],
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  },
  "error": null
}
```

---

### 目的地详情

获取单个目的地的详细信息，包含关联的景点列表。

- **URL**: `GET /api/v1/destinations/{id}`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 目的地 ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/destinations/1"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "杭州",
    "country": "中国",
    "province": "浙江省",
    "description": "上有天堂，下有苏杭",
    "bestSeason": "SPRING",
    "coverUrl": "https://example.com/covers/hangzhou.jpg",
    "imageUrls": "[\"url1\",\"url2\"]",
    "attractions": [
      {
        "id": 101,
        "destinationId": 1,
        "name": "西湖",
        "description": "世界文化遗产",
        "location": "杭州市西湖区",
        "ticketPrice": 0.00,
        "coverUrl": "https://example.com/covers/west-lake.jpg",
        "imageUrls": "[\"url1\",\"url2\"]",
        "createdAt": "2024-01-15T10:00:00"
      },
      {
        "id": 102,
        "destinationId": 1,
        "name": "灵隐寺",
        "description": "千年古刹",
        "location": "杭州市西湖区灵隐路",
        "ticketPrice": 75.00,
        "coverUrl": "https://example.com/covers/lingyin.jpg",
        "imageUrls": "[\"url1\",\"url2\"]",
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "createdAt": "2024-01-01T10:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 目的地不存在 (404)
{
  "success": false,
  "data": null,
  "error": "目的地不存在"
}
```

---

## 景点接口

### 景点列表

分页查询景点列表，支持按目的地、关键词筛选。

- **URL**: `GET /api/v1/attractions`
- **认证**: 无需

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从 1 开始 |
| limit | int | 否 | 20 | 每页数量，最大 100 |
| destinationId | long | 否 | — | 按所属目的地筛选 |
| keyword | string | 否 | — | 按名称关键词搜索 |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/attractions?page=1&limit=10&destinationId=1"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 101,
        "destinationId": 1,
        "name": "西湖",
        "description": "世界文化遗产",
        "location": "杭州市西湖区",
        "ticketPrice": 0.00,
        "coverUrl": "https://example.com/covers/west-lake.jpg",
        "imageUrls": "[\"url1\",\"url2\"]",
        "createdAt": "2024-01-15T10:00:00"
      }
    ],
    "total": 30,
    "size": 10,
    "current": 1,
    "pages": 3
  },
  "error": null
}
```

---

### 景点详情

获取单个景点的详细信息。

- **URL**: `GET /api/v1/attractions/{id}`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 景点 ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/attractions/101"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 101,
    "destinationId": 1,
    "name": "西湖",
    "description": "世界文化遗产，国家重点风景名胜区",
    "location": "杭州市西湖区龙井路1号",
    "ticketPrice": 0.00,
    "coverUrl": "https://example.com/covers/west-lake.jpg",
    "imageUrls": "[\"url1\",\"url2\",\"url3\"]",
    "createdAt": "2024-01-15T10:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 景点不存在 (404)
{
  "success": false,
  "data": null,
  "error": "景点不存在"
}
```

---

## 攻略接口

### 攻略列表

分页查询已发布的攻略列表，支持按目的地、作者、关键词筛选和排序。

- **URL**: `GET /api/v1/guides`
- **认证**: 无需

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从 1 开始 |
| limit | int | 否 | 20 | 每页数量，最大 100 |
| destinationId | long | 否 | — | 按关联目的地筛选 |
| authorId | long | 否 | — | 按作者筛选 |
| keyword | string | 否 | — | 按标题关键词搜索 |
| sortBy | string | 否 | `create_time` | 排序方式，见 [排序方式](#排序方式) |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/guides?page=1&limit=10&sortBy=create_time"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "authorId": 5,
        "title": "杭州三日游攻略",
        "coverUrl": "https://example.com/guides/hangzhou-3d.jpg",
        "destinationName": "杭州",
        "viewCount": 1200,
        "likeCount": 85,
        "createdAt": "2024-03-15T10:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "error": null
}
```

---

### 攻略详情

获取攻略详细信息，包含完整正文内容。访问后浏览量自动 +1。

- **URL**: `GET /api/v1/guides/{id}`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 攻略 ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/guides/1"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "authorId": 5,
    "title": "杭州三日游攻略",
    "content": "# Day 1: 西湖环线\n\n第一天从断桥出发...\n\n# Day 2: 灵隐寺\n\n...",
    "destinationId": 1,
    "destinationName": "杭州",
    "coverUrl": "https://example.com/guides/hangzhou-3d.jpg",
    "imageUrls": "[\"url1\",\"url2\"]",
    "status": "PUBLISHED",
    "viewCount": 1201,
    "likeCount": 85,
    "createdAt": "2024-03-15T10:00:00",
    "updatedAt": "2024-03-15T10:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 攻略不存在 (404)
{
  "success": false,
  "data": null,
  "error": "攻略不存在"
}
```

---

### 发布攻略

创建一篇新的攻略。

- **URL**: `POST /api/v1/guides`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "title": "杭州三日游攻略",
  "content": "# Day 1: 西湖环线\n\n第一天从断桥出发...",
  "destinationId": 1,
  "coverUrl": "https://example.com/covers/hangzhou-guide.jpg",
  "imageUrls": "[\"url1\",\"url2\",\"url3\"]",
  "status": "PUBLISHED"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 攻略标题，最长 200 字符 |
| content | string | 是 | 攻略正文，支持 Markdown 格式 |
| destinationId | long | 否 | 关联目的地 ID（可为 null，纯经验贴） |
| coverUrl | string | 否 | 封面图 URL，最长 500 字符 |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |
| status | string | 否 | 发布状态：`DRAFT`（草稿）/ `PUBLISHED`（已发布），默认 `DRAFT` |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/guides" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "杭州三日游攻略",
    "content": "# Day 1: 西湖环线\n\n第一天从断桥出发...",
    "destinationId": 1,
    "status": "PUBLISHED"
  }'
```

#### 成功响应 (201)

```json
{
  "success": true,
  "data": {
    "id": 2,
    "authorId": 5,
    "title": "杭州三日游攻略",
    "content": "# Day 1: 西湖环线\n\n第一天从断桥出发...",
    "destinationId": 1,
    "destinationName": "杭州",
    "coverUrl": null,
    "imageUrls": null,
    "status": "PUBLISHED",
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": "2024-06-01T10:00:00",
    "updatedAt": "2024-06-01T10:00:00"
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
  "error": "攻略标题不能为空"
}
```

---

### 编辑攻略

修改已发布的攻略，仅作者可操作。

- **URL**: `PUT /api/v1/guides/{id}`
- **认证**: 需要 JWT Token（仅作者）

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 攻略 ID |

#### 请求参数

所有字段均为可选，仅传入需要修改的字段。

```json
{
  "title": "杭州四日游攻略（更新版）",
  "content": "更新后的正文内容...",
  "status": "PUBLISHED"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 攻略标题，最长 200 字符 |
| content | string | 否 | 攻略正文 |
| destinationId | long | 否 | 关联目的地 ID |
| coverUrl | string | 否 | 封面图 URL |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |
| status | string | 否 | 发布状态 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/guides/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "杭州四日游攻略（更新版）"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "authorId": 5,
    "title": "杭州四日游攻略（更新版）",
    "content": "...",
    "destinationId": 1,
    "destinationName": "杭州",
    "coverUrl": null,
    "imageUrls": null,
    "status": "PUBLISHED",
    "viewCount": 1201,
    "likeCount": 85,
    "createdAt": "2024-03-15T10:00:00",
    "updatedAt": "2024-06-01T15:00:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 非作者操作 (403)
{
  "success": false,
  "data": null,
  "error": "无权限操作"
}

// 攻略不存在 (404)
{
  "success": false,
  "data": null,
  "error": "攻略不存在"
}
```

---

### 删除攻略

删除攻略，仅作者可操作。

- **URL**: `DELETE /api/v1/guides/{id}`
- **认证**: 需要 JWT Token（仅作者）

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 攻略 ID |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/guides/1" \
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

#### 失败响应

```json
// 非作者操作 (403)
{
  "success": false,
  "data": null,
  "error": "无权限操作"
}
```

---

### 点赞攻略

对攻略进行点赞。

- **URL**: `POST /api/v1/guides/{id}/like`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 攻略 ID |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/guides/1/like" \
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

### 取消点赞

取消对攻略的点赞。

- **URL**: `DELETE /api/v1/guides/{id}/like`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 攻略 ID |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/guides/1/like" \
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

## 评论接口

> 评论接口挂载在攻略路径下：`/api/v1/guides/{guideId}/comments`

### 评论列表

查询指定攻略的评论列表，以树形结构返回（支持嵌套回复）。

- **URL**: `GET /api/v1/guides/{guideId}/comments`
- **认证**: 无需

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| guideId | long | 攻略 ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/guides/1/comments"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 10,
      "parentId": null,
      "content": "写得太好了，收藏！",
      "likeCount": 5,
      "createdAt": "2024-03-16T09:00:00",
      "children": [
        {
          "id": 2,
          "userId": 5,
          "parentId": 1,
          "content": "谢谢支持~",
          "likeCount": 1,
          "createdAt": "2024-03-16T10:00:00",
          "children": []
        }
      ]
    },
    {
      "id": 3,
      "userId": 15,
      "parentId": null,
      "content": "请问第三天行程可以松一点吗？",
      "likeCount": 0,
      "createdAt": "2024-03-17T14:00:00",
      "children": []
    }
  ],
  "error": null
}
```

---

### 发表评论

对攻略发表评论或回复某条评论。

- **URL**: `POST /api/v1/guides/{guideId}/comments`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| guideId | long | 攻略 ID |

#### 请求参数

```json
{
  "content": "写得太好了，收藏！",
  "parentId": null
}
```

```json
// 回复某条评论
{
  "content": "谢谢你的建议！",
  "parentId": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 评论内容 |
| parentId | long | 否 | 父评论 ID，为 null 时表示顶级评论 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/guides/1/comments" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "content": "非常详细的攻略，谢谢分享！",
    "parentId": null
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 4,
    "userId": 8,
    "parentId": null,
    "content": "非常详细的攻略，谢谢分享！",
    "likeCount": 0,
    "createdAt": "2024-06-01T16:00:00",
    "children": []
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
  "error": "评论内容不能为空"
}
```

---

### 删除评论

删除评论，仅评论作者可操作。

- **URL**: `DELETE /api/v1/guides/{guideId}/comments/{commentId}`
- **认证**: 需要 JWT Token（仅评论作者）

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| guideId | long | 攻略 ID |
| commentId | long | 评论 ID |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/guides/1/comments/4" \
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

#### 失败响应

```json
// 非作者操作 (403)
{
  "success": false,
  "data": null,
  "error": "无权限操作"
}
```

---

## 上传接口

> 以下接口均需在请求头中携带有效的 Access Token。

### 上传图片

上传单张图片到服务器。

- **URL**: `POST /api/v1/uploads/image`
- **认证**: 需要 JWT Token
- **Content-Type**: `multipart/form-data`

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/uploads/image" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/image.jpg"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "url": "/uploads/images/abc123.jpg"
  },
  "error": null
}
```

---

### 批量上传图片

批量上传多张图片到服务器。

- **URL**: `POST /api/v1/uploads/images`
- **认证**: 需要 JWT Token
- **Content-Type**: `multipart/form-data`

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | file[] | 是 | 图片文件数组 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/uploads/images" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg" \
  -F "files=@/path/to/image3.jpg"
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "urls": [
      "/uploads/images/abc123.jpg",
      "/uploads/images/def456.jpg",
      "/uploads/images/ghi789.jpg"
    ]
  },
  "error": null
}
```

---

### 删除图片

删除已上传的图片。

- **URL**: `DELETE /api/v1/uploads/{filename}`
- **认证**: 需要 JWT Token

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| filename | string | 文件名（禁止包含 `../`、`/`、`\\` 等路径穿越字符） |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/uploads/abc123.jpg" \
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

#### 失败响应

```json
// 非法文件名 (401)
{
  "success": false,
  "data": null,
  "error": "非法文件名"
}
```

---

## 管理端 — 目的地

> 以下接口需要 **ADMIN** 或 **CONTENT_OPERATOR** 角色。

### 创建目的地

创建新的目的地。

- **URL**: `POST /api/v1/admin/destinations`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 请求参数

```json
{
  "name": "杭州",
  "country": "中国",
  "province": "浙江省",
  "description": "上有天堂，下有苏杭",
  "bestSeason": "SPRING",
  "coverUrl": "https://example.com/covers/hangzhou.jpg",
  "imageUrls": "[\"url1\",\"url2\"]"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 目的地名称，最长 100 字符 |
| country | string | 是 | 所属国家，最长 60 字符 |
| province | string | 否 | 所属省份/州，最长 60 字符 |
| description | string | 否 | 目的地简介 |
| bestSeason | string | 否 | 最佳旅游季节，见 [季节枚举](#季节枚举) |
| coverUrl | string | 否 | 封面图 URL，最长 500 字符 |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/admin/destinations" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "杭州",
    "country": "中国",
    "province": "浙江省",
    "description": "上有天堂，下有苏杭",
    "bestSeason": "SPRING"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "杭州",
    "country": "中国",
    "province": "浙江省",
    "description": "上有天堂，下有苏杭",
    "bestSeason": "SPRING",
    "coverUrl": null,
    "imageUrls": null,
    "attractions": [],
    "createdAt": "2024-06-01T10:00:00"
  },
  "error": null
}
```

---

### 更新目的地

更新目的地的信息。

- **URL**: `PUT /api/v1/admin/destinations/{id}`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 目的地 ID |

#### 请求参数

所有字段均为可选。

```json
{
  "name": "新名称",
  "description": "更新后的简介"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 目的地名称 |
| country | string | 否 | 所属国家 |
| province | string | 否 | 所属省份 |
| description | string | 否 | 目的地简介 |
| bestSeason | string | 否 | 最佳旅游季节 |
| coverUrl | string | 否 | 封面图 URL |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/admin/destinations/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "description": "更新后的简介"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "杭州",
    "country": "中国",
    "province": "浙江省",
    "description": "更新后的简介",
    "bestSeason": "SPRING",
    "coverUrl": null,
    "imageUrls": null,
    "attractions": [],
    "createdAt": "2024-06-01T10:00:00"
  },
  "error": null
}
```

---

### 删除目的地

删除目的地及其关联的景点。

- **URL**: `DELETE /api/v1/admin/destinations/{id}`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 目的地 ID |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/admin/destinations/1" \
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

## 管理端 — 景点

> 以下接口需要 **ADMIN** 或 **CONTENT_OPERATOR** 角色。

### 创建景点

在指定目的地下面创建新景点。

- **URL**: `POST /api/v1/admin/attractions`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 请求参数

```json
{
  "destinationId": 1,
  "name": "西湖",
  "description": "世界文化遗产",
  "location": "杭州市西湖区",
  "ticketPrice": 0.00,
  "coverUrl": "https://example.com/covers/west-lake.jpg",
  "imageUrls": "[\"url1\",\"url2\"]"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| destinationId | long | 是 | 所属目的地 ID |
| name | string | 是 | 景点名称，最长 200 字符 |
| description | string | 否 | 景点简介 |
| location | string | 否 | 详细地址，最长 300 字符 |
| ticketPrice | number | 否 | 门票价格，null 表示免费 |
| coverUrl | string | 否 | 封面图 URL，最长 500 字符 |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/api/v1/admin/attractions" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "destinationId": 1,
    "name": "西湖",
    "description": "世界文化遗产",
    "location": "杭州市西湖区",
    "ticketPrice": 0.00
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 101,
    "destinationId": 1,
    "name": "西湖",
    "description": "世界文化遗产",
    "location": "杭州市西湖区",
    "ticketPrice": 0.00,
    "coverUrl": null,
    "imageUrls": null,
    "createdAt": "2024-06-01T10:00:00"
  },
  "error": null
}
```

---

### 更新景点

更新景点信息。

- **URL**: `PUT /api/v1/admin/attractions/{id}`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 景点 ID |

#### 请求参数

所有字段均为可选。

```json
{
  "name": "新名称",
  "ticketPrice": 50.00
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 景点名称 |
| description | string | 否 | 景点简介 |
| location | string | 否 | 详细地址 |
| ticketPrice | number | 否 | 门票价格 |
| coverUrl | string | 否 | 封面图 URL |
| imageUrls | string | 否 | 多图 JSON 数组字符串 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/admin/attractions/101" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "ticketPrice": 50.00
  }'
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

### 删除景点

删除景点。

- **URL**: `DELETE /api/v1/admin/attractions/{id}`
- **认证**: 需要 JWT Token + ADMIN / CONTENT_OPERATOR 角色

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 景点 ID |

#### 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/v1/admin/attractions/101" \
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

## 附录

### 季节枚举

| 值 | 说明 |
|----|------|
| SPRING | 春季（3-5月） |
| SUMMER | 夏季（6-8月） |
| AUTUMN | 秋季（9-11月） |
| WINTER | 冬季（12-2月） |
| YEAR_ROUND | 全年适宜 |

### 攻略状态枚举

| 值 | 说明 |
|----|------|
| DRAFT | 草稿，仅作者可见 |
| PUBLISHED | 已发布，公开可见（列表查询仅返回已发布） |
| REJECTED | 审核驳回 |

### 排序方式

攻略列表接口 `sortBy` 参数支持以下值：

| 值 | 说明 |
|----|------|
| `create_time` | 按创建时间倒序（默认） |
