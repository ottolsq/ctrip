# 捷程旅行网 API 开发规范

> 本规范定义了项目的 API 设计标准，所有新增接口必须遵循。

---

## 1. 统一响应格式

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

### 1.1 响应字段约定

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| success | boolean | 是 | 请求是否成功 |
| data | object/array/null | 是 | 业务数据，失败时为 null |
| error | string/null | 是 | 错误描述，成功时为 null |

### 1.2 分页响应格式

列表接口返回分页数据时，`data` 中包含以下结构：

```json
{
  "success": true,
  "data": {
    "records": [ /* 数据列表 */ ],
    "total": 100,
    "page": 1,
    "limit": 20,
    "pages": 5
  },
  "error": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| records | array | 当前页数据列表 |
| total | number | 总记录数 |
| page | number | 当前页码（从 1 开始） |
| limit | number | 每页条数 |
| pages | number | 总页数 |

### 1.3 分页参数约定

- 分页参数通过 Query String 传递：`?page=1&limit=20`
- 默认 `page=1`，默认 `limit=20`，最大 `limit=100`
- 参数名统一为 `page` 和 `limit`

---

## 2. URL 设计规范

### 2.1 基础路径

所有 API 接口均以 `/api/v1` 为前缀，版本号为路径的一部分。

### 2.2 命名规范

- 资源使用 **小写复数** 形式：`/api/v1/users`、`/api/v1/destinations`
- 嵌套资源使用路径层级：`/api/v1/guides/{id}/comments`
- 子资源操作使用父资源路径：`/api/v1/itineraries/{id}/days`
- 管理端接口使用 `/api/v1/admin/` 前缀：`/api/v1/admin/destinations`

### 2.3 常用路径模式

| 操作 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 列表 | GET | `/api/v1/{resource}` | 支持分页、筛选 |
| 详情 | GET | `/api/v1/{resource}/{id}` | 返回单条记录 |
| 创建 | POST | `/api/v1/{resource}` | 请求体包含创建数据 |
| 更新 | PUT | `/api/v1/{resource}/{id}` | 全量/部分更新 |
| 删除 | DELETE | `/api/v1/{resource}/{id}` | 软删除 |
| 子资源列表 | GET | `/api/v1/{resource}/{id}/{sub}` | 嵌套列表 |
| 子资源创建 | POST | `/api/v1/{resource}/{id}/{sub}` | 嵌套创建 |

---

## 3. HTTP 状态码规范

| HTTP 状态码 | 含义 | 使用场景 |
|------------|------|---------|
| 200 | OK | 请求成功（查询、更新） |
| 201 | Created | 资源创建成功（注册、创建订单等） |
| 400 | Bad Request | 请求参数校验失败 |
| 401 | Unauthorized | 未登录或 Token 无效/过期 |
| 403 | Forbidden | 权限不足（非资源所有者尝试操作） |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如用户名已存在、重复操作） |
| 429 | Too Many Requests | 请求频率超限（限流） |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 4. 认证与授权

### 4.1 认证方式

需要认证的接口必须在请求头中携带 Access Token：

```
Authorization: Bearer <access_token>
```

### 4.2 Token 类型

| Token 类型 | 有效期 | 说明 |
|-----------|-------|------|
| Access Token | 15 分钟 | 用于接口鉴权，过期后需刷新 |
| Refresh Token | 7 天 | 用于刷新 Access Token，采用轮换策略 |

### 4.3 Token 轮换策略

- 每次调用 `/api/v1/auth/refresh` 都会吊销旧的 Refresh Token
- 返回新的 Access Token + 新的 Refresh Token
- 客户端必须保存新返回的 Refresh Token，旧 Token 失效

### 4.4 认证级别

| 接口类型 | 认证要求 | 说明 |
|---------|---------|------|
| 公开接口 | 无需认证 | 如登录、注册、忘记密码 |
| 用户接口 | 需要 JWT | 如个人资料、行程管理 |
| 管理端接口 | 需要 JWT + ADMIN 角色 | 如内容审核、用户管理 |

---

## 5. 错误处理

### 5.1 自定义异常类型

| 异常类 | HTTP 状态码 | 使用场景 |
|--------|-----------|---------|
| AuthenticationException | 401 | 认证失败（密码错误、Token 无效等） |
| ResourceNotFoundException | 404 | 资源不存在 |
| DuplicateResourceException | 409 | 重复资源（手机号/邮箱已注册等） |
| TokenExpiredException | 401 | Token 过期 |
| BusinessException | 400/500 | 业务异常（OTP 无效、账号封禁等） |

### 5.2 错误响应格式

```json
{
  "success": false,
  "data": null,
  "error": "具体的错误描述信息"
}
```

### 5.3 防枚举攻击

- 登录/忘记密码/重置密码接口：用户不存在时返回通用错误，不暴露用户是否存在
- 登录接口：先检查账号状态（封禁），再验证密码，确保封禁和密码错误的响应一致

---

## 6. 限流规范

### 6.1 限流端点

| 端点 | 限制 | 说明 |
|------|------|------|
| `POST /auth/login` | 5 次/分钟/IP | 防暴力破解 |
| `POST /auth/register/**` | 3 次/分钟/IP | 防恶意注册 |
| `POST /auth/password/forgot` | 3 次/分钟/IP | 防短信轰炸 |
| `POST /auth/sms/send` | 2 次/分钟/IP | 防短信轰炸 |
| `POST /auth/refresh` | 10 次/分钟/IP | 防令牌滥用 |

### 6.2 限流响应

超过限流阈值后，返回 HTTP 429：

```json
{
  "success": false,
  "data": null,
  "error": "请求过于频繁，请稍后再试"
}
```

---

## 7. 数据格式约定

### 7.1 日期时间

- 日期：`YYYY-MM-DD`（如 `1990-01-15`）
- 日期时间：ISO 8601 格式（如 `2024-01-01T10:30:00`）

### 7.2 枚举值

- 使用大写字符串：`MALE`、`FEMALE`、`ACTIVE`、`PUBLISHED`
- 字段名使用 camelCase

### 7.3 文件上传

- 单张上传：`POST /api/v1/uploads/image`，返回 `{ "url": "..." }`
- 批量上传：`POST /api/v1/uploads/images`，返回 `{ "urls": ["url1", "url2", ...] }`
- 单张大小限制：5MB，支持 jpg/png/webp 格式

---

## 8. API 文档规范

每个接口文档必须包含：

1. **接口描述**：一句话说明接口用途
2. **URL + Method**：如 `POST /api/v1/auth/login`
3. **认证要求**：是否需要 JWT Token
4. **限流配置**：如有
5. **请求参数**：表格列出参数名、类型、是否必填、说明
6. **请求示例**：JSON 格式的请求体示例
7. **成功响应**：状态码 + 响应体示例
8. **失败响应**：各错误场景的状态码 + 响应体示例
9. **前置条件**（如有）：如「需先调用 XX 接口获取验证码」
