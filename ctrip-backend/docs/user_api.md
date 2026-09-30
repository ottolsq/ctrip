# 捷程旅行网 API 接口文档

> 所有接口均返回统一的 JSON 响应格式，基础路径为 `/api/v1`。

---

## 目录

- [通用说明](#通用说明)
  - [响应格式](#响应格式)
  - [认证方式](#认证方式)
  - [错误码说明](#错误码说明)
- [认证接口](#认证接口)
  - [手机号注册](#手机号注册)
  - [邮箱注册](#邮箱注册)
  - [登录](#登录)
  - [刷新令牌](#刷新令牌)
  - [登出](#登出)
  - [发送短信验证码](#发送短信验证码)
  - [忘记密码](#忘记密码)
  - [重置密码](#重置密码)
- [用户接口](#用户接口)
  - [获取个人资料](#获取个人资料)
  - [更新个人资料](#更新个人资料)
  - [修改密码](#修改密码)
  - [更新头像](#更新头像)

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

- **认证接口**（`/api/v1/auth/**`）：无需携带 JWT Token
- **用户接口**（`/api/v1/users/**`）：必须在请求头中携带有效的 Access Token

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
| 201 | Created | 资源创建成功（注册接口） |
| 400 | Bad Request | 请求参数校验失败 |
| 401 | Unauthorized | 未登录或 Token 无效/过期 |
| 403 | Forbidden | 权限不足 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如用户名已存在） |
| 429 | Too Many Requests | 请求频率超限 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 认证接口

### 手机号注册

通过手机号 + 短信验证码完成注册，注册成功后自动激活账号并颁发令牌对。

**前置条件**：需先调用 [发送短信验证码](#发送短信验证码) 接口获取验证码。

- **URL**: `POST /api/v1/auth/register/phone`
- **认证**: 无需
- **限流**: 3 次/分钟/IP

#### 请求参数

```json
{
  "phone": "13800138000",
  "smsCode": "123456",
  "password": "SecurePass123",
  "username": "张三"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号，格式：可选 `+` 前缀，7-15 位数字 |
| smsCode | string | 是 | 短信验证码，固定 6 位数字 |
| password | string | 是 | 登录密码，8-64 位 |
| username | string | 是 | 用户名，2-50 位 |

#### 成功响应 (201)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "error": null
}
```

#### 失败响应

```json
// 验证码错误 (400)
{
  "success": false,
  "data": null,
  "error": "验证码无效或已过期"
}

// 手机号已注册 (409)
{
  "success": false,
  "data": null,
  "error": "该手机号已被注册"
}

// 用户名已存在 (409)
{
  "success": false,
  "data": null,
  "error": "用户名已被使用"
}
```

---

### 邮箱注册

通过邮箱 + 密码完成注册，注册后账号状态为 `UNVERIFIED`（未验证），但仍可正常使用系统功能。

**注意**：邮件验证功能待后续版本实现。

- **URL**: `POST /api/v1/auth/register/email`
- **认证**: 无需
- **限流**: 3 次/分钟/IP

#### 请求参数

```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "username": "张三"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址，需符合邮箱格式 |
| password | string | 是 | 登录密码，8-64 位 |
| username | string | 是 | 用户名，2-50 位 |

#### 成功响应 (201)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "error": null
}
```

#### 失败响应

```json
// 邮箱已注册 (409)
{
  "success": false,
  "data": null,
  "error": "该邮箱已被注册"
}

// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "邮箱格式不正确"
}
```

---

### 登录

使用手机号或邮箱 + 密码登录，系统自动识别凭据类型。

- **URL**: `POST /api/v1/auth/login`
- **认证**: 无需
- **限流**: 5 次/分钟/IP

#### 请求参数

```json
{
  "credential": "user@example.com",
  "password": "SecurePass123"
}
```

```json
// 或使用手机号登录
{
  "credential": "13800138000",
  "password": "SecurePass123"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| credential | string | 是 | 登录凭据：邮箱或手机号（含 `@` 则按邮箱处理，否则按手机号处理） |
| password | string | 是 | 登录密码 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "error": null
}
```

#### 失败响应

```json
// 凭据或密码错误 (401)
{
  "success": false,
  "data": null,
  "error": "用户名或密码错误"
}

// 账号已被封禁 (403)
{
  "success": false,
  "data": null,
  "error": "账号已被封禁，请联系客服"
}
```

---

### 刷新令牌

使用 Refresh Token 获取新的 Access Token。采用**令牌轮换策略**：每次刷新都会吊销旧的 Refresh Token 并颁发新的令牌对。

- **URL**: `POST /api/v1/auth/refresh`
- **认证**: 无需
- **限流**: 10 次/分钟/IP

#### 请求参数

```json
{
  "refreshToken": "a1b2c3d4e5f6..."
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 登录/注册时获取的 Refresh Token 原始值 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "newRefreshToken...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "error": null
}
```

**注意**：客户端必须保存新返回的 `refreshToken`，旧的 Refresh Token 已失效。

#### 失败响应

```json
// Token 无效或已过期 (401)
{
  "success": false,
  "data": null,
  "error": "Refresh token 无效或已过期"
}

// Token 已被吊销（登出后）(401)
{
  "success": false,
  "data": null,
  "error": "Refresh token 无效或已过期"
}
```

---

### 登出

吊销指定的 Refresh Token。这是一个**幂等操作**，重复调用不会报错。

**注意**：登出后，客户端应同时丢弃本地存储的 Access Token 和 Refresh Token。

- **URL**: `POST /api/v1/auth/logout`
- **认证**: 无需

#### 请求参数

```json
{
  "refreshToken": "a1b2c3d4e5f6..."
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 要吊销的 Refresh Token |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "登出成功"
  },
  "error": null
}
```

---

### 发送短信验证码

向指定手机号发送 6 位数字验证码，用于手机号注册。

**开发阶段说明**：当前版本不真正发送短信，验证码通过应用日志输出（DEBUG 级别）。启动应用后，在日志中搜索 `[StubSms]` 即可找到验证码。

- **URL**: `POST /api/v1/auth/sms/send`
- **认证**: 无需
- **限流**: 2 次/分钟/IP

#### 请求参数

```json
{
  "phone": "13800138000"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 接收验证码的手机号 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "验证码已发送"
  },
  "error": null
}
```

#### 失败响应

```json
// 手机号格式错误 (400)
{
  "success": false,
  "data": null,
  "error": "手机号格式不正确"
}
```

---

### 忘记密码

向用户注册时使用的邮箱或手机号发送一次性验证码（OTP），用于密码重置。

**防用户枚举**：无论账号是否存在，接口均返回相同的成功响应，防止攻击者枚举系统中的用户。

- **URL**: `POST /api/v1/auth/password/forgot`
- **认证**: 无需
- **限流**: 3 次/分钟/IP

#### 请求参数

```json
{
  "credential": "user@example.com"
}
```

```json
// 或使用手机号
{
  "credential": "13800138000"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| credential | string | 是 | 注册时使用的邮箱或手机号 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "若账号存在，验证码已发送"
  },
  "error": null
}
```

**说明**：即使账号不存在，也会返回此响应，防止用户枚举。

---

### 重置密码

使用 OTP（一次性验证码）和新密码完成密码重置。验证通过后，将更新密码并吊销该用户的所有 Refresh Token（强制重新登录）。

**前置条件**：需先调用 [忘记密码](#忘记密码) 接口获取 OTP。

- **URL**: `POST /api/v1/auth/password/reset`
- **认证**: 无需

#### 请求参数

```json
{
  "credential": "user@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePass123"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| credential | string | 是 | 注册时使用的邮箱或手机号 |
| otp | string | 是 | 收到的一次性验证码，6 位数字 |
| newPassword | string | 是 | 新密码，8-64 位 |

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "密码重置成功，请重新登录"
  },
  "error": null
}
```

#### 失败响应

```json
// OTP 无效或已过期 (400)
{
  "success": false,
  "data": null,
  "error": "OTP 无效或已过期"
}
```

---

## 用户接口

> 以下接口均需在请求头中携带有效的 Access Token：
> ```
> Authorization: Bearer <access_token>
> ```

### 获取个人资料

获取当前登录用户的个人资料信息。

- **URL**: `GET /api/v1/users/me`
- **认证**: 需要 JWT Token

#### 请求示例

```bash
curl -X GET "http://localhost:8080/api/v1/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "张三",
    "email": "user@example.com",
    "phone": "138****8000",
    "avatarUrl": "https://example.com/avatar/123.jpg",
    "gender": "MALE",
    "birthday": "1990-01-15",
    "realName": "张三",
    "emailVerified": false,
    "phoneVerified": true,
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2024-01-01T10:30:00"
  },
  "error": null
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱地址（可能为 null） |
| phone | string | 手机号（可能为 null，部分隐藏） |
| avatarUrl | string | 头像 URL（可能为 null） |
| gender | string | 性别：`MALE` / `FEMALE` / `UNSPECIFIED` |
| birthday | string | 生日，格式 `YYYY-MM-DD`（可能为 null） |
| realName | string | 实名认证姓名（可能为 null） |
| emailVerified | boolean | 邮箱是否已验证 |
| phoneVerified | boolean | 手机是否已验证 |
| status | string | 账号状态：`UNVERIFIED` / `ACTIVE` / `SUSPENDED` / `DELETED` |
| role | string | 用户角色：`USER` / `ADMIN` / `CONTENT_OPERATOR` |
| createdAt | string | 注册时间，ISO 8601 格式 |

#### 失败响应

```json
// 未登录 (401)
{
  "success": false,
  "data": null,
  "error": "请先登录"
}
```

---

### 更新个人资料

更新当前用户的个人资料信息。所有字段均为可选，仅传入需要修改的字段。

- **URL**: `PUT /api/v1/users/me`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "username": "新用户名",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "gender": "MALE",
  "birthday": "1990-01-15",
  "realName": "实名姓名"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 用户名，2-50 位 |
| avatarUrl | string | 否 | 头像 URL，最长 512 位 |
| gender | string | 否 | 性别：`MALE` / `FEMALE` / `UNSPECIFIED` |
| birthday | string | 否 | 生日，格式 `YYYY-MM-DD` |
| realName | string | 否 | 实名认证姓名，最长 50 位 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "username": "新用户名",
    "gender": "MALE"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "新用户名",
    "email": "user@example.com",
    "phone": "138****8000",
    "avatarUrl": "https://example.com/avatar/123.jpg",
    "gender": "MALE",
    "birthday": "1990-01-15",
    "realName": "实名姓名",
    "emailVerified": false,
    "phoneVerified": true,
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2024-01-01T10:30:00"
  },
  "error": null
}
```

#### 失败响应

```json
// 用户名已被使用 (409)
{
  "success": false,
  "data": null,
  "error": "用户名已被使用"
}

// 参数校验失败 (400)
{
  "success": false,
  "data": null,
  "error": "用户名长度必须在 2-50 位之间"
}
```

---

### 修改密码

修改当前用户的登录密码。需要提供当前密码进行身份二次确认，防止会话被劫持后密码被静默修改。

**注意**：修改密码成功后，所有已登录设备上的 Refresh Token 都会被吊销，需要重新登录。

- **URL**: `PUT /api/v1/users/me/password`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewSecurePass456"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| currentPassword | string | 是 | 当前密码（用于身份确认） |
| newPassword | string | 是 | 新密码，8-64 位 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/users/me/password" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "OldPass123",
    "newPassword": "NewSecurePass456"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "message": "密码修改成功"
  },
  "error": null
}
```

#### 失败响应

```json
// 当前密码错误 (401)
{
  "success": false,
  "data": null,
  "error": "当前密码错误"
}

// 新密码格式错误 (400)
{
  "success": false,
  "data": null,
  "error": "新密码长度必须在 8-64 位之间"
}
```

---

### 更新头像

单独更新当前用户的头像 URL。

- **URL**: `PUT /api/v1/users/me/avatar`
- **认证**: 需要 JWT Token

#### 请求参数

```json
{
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| avatarUrl | string | 是 | 新头像 URL，最长 512 位 |

#### 请求示例

```bash
curl -X PUT "http://localhost:8080/api/v1/users/me/avatar" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "avatarUrl": "https://example.com/new-avatar.jpg"
  }'
```

#### 成功响应 (200)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "张三",
    "email": "user@example.com",
    "phone": "138****8000",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "gender": "MALE",
    "birthday": "1990-01-15",
    "realName": "实名姓名",
    "emailVerified": false,
    "phoneVerified": true,
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2024-01-01T10:30:00"
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
  "error": "头像地址不能为空"
}
```

---

## 附录

### 性别枚举值

| 值 | 说明 |
|----|------|
| MALE | 男性 |
| FEMALE | 女性 |
| UNSPECIFIED | 未指定 |

### 账号状态枚举值

| 值 | 说明 |
|----|------|
| UNVERIFIED | 未验证（邮箱注册后初始状态） |
| ACTIVE | 正常 |
| SUSPENDED | 已封禁 |
| DELETED | 已注销 |

### 角色枚举值

| 值 | 说明 | 权限 |
|----|------|------|
| USER | 普通用户（默认角色） | 发布攻略、评论、收藏、行程、盲盒 |
| ADMIN | 管理员 | 所有管理端接口 + 内容审核 |
| CONTENT_OPERATOR | 内容运维 | 攻略审核、评论管理、目的地/景点 CRUD |

**说明**：角色信息通过 JWT token 中的 `role` claim 传递，用于接口权限校验。访问 `/api/v1/admin/**` 等管理端接口需要 ADMIN 或 CONTENT_OPERATOR 角色，否则返回 403。

### 限流配置汇总

| 端点 | 限制 |
|------|------|
| `POST /auth/login` | 5 次/分钟/IP |
| `POST /auth/register/**` | 3 次/分钟/IP |
| `POST /auth/password/forgot` | 3 次/分钟/IP |
| `POST /auth/sms/send` | 2 次/分钟/IP |
| `POST /auth/refresh` | 10 次/分钟/IP |

超过限流阈值后，接口返回 HTTP 429：

```json
{
  "success": false,
  "data": null,
  "error": "请求过于频繁，请稍后再试"
}
```
