# 趣行旅行平台 管理端 API（P1.1）

> 前缀：`/api/v1/admin`
> 认证：JWT Bearer Token；本阶段各端点要求 `ADMIN` 角色。
> 响应：统一 `ApiResponse<T>`（`success/data/error`）。

## 1. 权限与种子账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin@example.com | Admin12345 | ADMIN | 全部管理功能 |
| operator@example.com | Operator12345 | CONTENT_OPERATOR | 内容运营（P1.2 起开放） |

种子账号由 `docs/SQL/ctrip_admin_seed.sql` 写入，仅限本地开发/答辩演示。

- `/api/v1/admin/**` 的 URL 基线允许 ADMIN/CONTENT_OPERATOR；
- 用户管理端点额外使用 `@PreAuthorize("hasRole('ADMIN')")`，内容运营访问返回 **403**。

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
