# 趣行旅行平台 API 契约

> 入库的唯一接口契约：通用约定 + 端点索引。请求/响应示例、字段明细和各模块实现说明在本地 `docs/api/`（不入库）。
> 新增或修改接口时先改本文件，再补本地明细；两处不一致以代码为准，并回来修本文件。

## 1. 基础约定

| 项 | 约定 |
|---|---|
| 前缀 | `/api/v1`，版本在路径里 |
| 资源命名 | 小写复数：`/api/v1/users`、`/api/v1/destinations` |
| 嵌套 | 父资源在路径里：`/api/v1/itineraries/{id}/days/{dayId}/items` |
| 管理端 | `/api/v1/admin/` 前缀 |
| 分页参数 | Query：`page`（默认 1）、`limit` 或 `size`（默认 20，最大 100） |
| 日期 | 日期 `YYYY-MM-DD`；时间 ISO 8601，如 `2024-01-01T10:30:00` |
| 枚举 | 大写字符串：`MALE`、`PUBLISHED`、`OPENED`；字段名 camelCase |
| 上传 | 单张 5MB，支持 jpg/png/webp；单张返回 `url`，批量返回 `urls` |
| 分享链接 | 行程分享与盲盒结果分享均为 7 天有效期，取消后立即失效 |

统一响应信封（所有接口）：

```json
{ "success": true, "data": {}, "error": null }
```

失败时 `success=false`、`data=null`、`error` 为可读错误描述。分页接口的 `data` 为 `{ records, total, page, limit, pages }`。

## 2. 认证与权限

- 需要认证的接口带 `Authorization: Bearer <access_token>`。
- Access Token 15 分钟；Refresh Token 15 天，刷新时轮换（旧值立即失效，每次刷新返回新的一对）。
- 角色三级：`USER`、`CONTENT_OPERATOR`、`ADMIN`。管理端接口在 URL 层放行 `ADMIN`/`CONTENT_OPERATOR`，具体方法再有注解限制；`ADMIN` 才有的操作在本文件标「管理员」。
- 登录/忘记密码/重置密码对「账号不存在」返回通用错误，不暴露账号是否存在；登录先判封禁再验密码。

限流（按 IP，超限返回 429）：

| 端点 | 限制 |
|---|---|
| `POST /auth/login` | 5 次/分钟 |
| `POST /auth/register/**` | 3 次/分钟 |
| `POST /auth/password/forgot` | 3 次/分钟 |
| `POST /auth/sms/send` | 2 次/分钟 |
| `POST /auth/refresh` | 10 次/分钟 |

## 3. 状态码与错误

| 状态码 | 含义 | 典型场景 |
|---|---|---|
| 200 | OK | 查询、更新 |
| 201 | Created | 注册、创建订单 |
| 400 | Bad Request | 参数校验失败、业务规则不满足 |
| 401 | Unauthorized | 未登录、Token 无效或过期 |
| 403 | Forbidden | 权限不足、非资源所有者 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 重复注册、重复操作 |
| 429 | Too Many Requests | 触发限流 |
| 500 | Internal Server Error | 服务端异常 |

业务异常与状态码的对应：`AuthenticationException`→401、`TokenExpiredException`→401、`ResourceNotFoundException`→404、`DuplicateResourceException`→409、`BusinessException`→400/500。

## 4. 端点索引

权限列是粗粒度标注：`公开` 无需登录，`登录` 需要 JWT，`管理员` 需要 `ADMIN`（标注为「运营」的还需要 `CONTENT_OPERATOR`）。精确校验以实现为准。

### 4.1 认证与用户

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/auth/register/phone` | 公开 | 手机号注册 |
| POST | `/auth/register/email` | 公开 | 邮箱注册 |
| POST | `/auth/login` | 公开 | 登录，返回双 Token |
| POST | `/auth/refresh` | 公开 | 刷新令牌并轮换 |
| POST | `/auth/logout` | 登录 | 登出并吊销 Refresh Token |
| POST | `/auth/sms/send` | 公开 | 发送短信验证码 |
| POST | `/auth/password/forgot` | 公开 | 忘记密码，发 OTP |
| POST | `/auth/password/reset` | 公开 | 用 OTP 重置密码 |
| GET | `/users/me` | 登录 | 个人资料 |
| PUT | `/users/me` | 登录 | 更新个人资料 |
| PUT | `/users/me/password` | 登录 | 修改密码 |
| PUT | `/users/me/avatar` | 登录 | 更新头像 |

### 4.2 内容：目的地、景点、攻略、评论、上传、收藏

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/destinations` | 公开 | 目的地列表，支持关键词筛选 |
| GET | `/destinations/{id}` | 公开 | 目的地详情 |
| GET | `/attractions` | 公开 | 景点列表 |
| GET | `/attractions/{id}` | 公开 | 景点详情 |
| GET | `/guides` | 公开 | 攻略列表 |
| GET | `/guides/{id}` | 公开 | 攻略详情 |
| POST | `/guides` | 登录 | 发布攻略 |
| PUT | `/guides/{id}` | 登录 | 编辑攻略（作者） |
| GET | `/guides/mine` | 登录 | 我的攻略，可按状态过滤 |
| GET | `/guides/{id}/manage` | 登录 | 作者管理视角详情 |
| POST | `/guides/{id}/submit` | 登录 | 提交审核 |
| DELETE | `/guides/{id}` | 登录 | 删除攻略（作者） |
| POST | `/guides/{id}/like` | 登录 | 点赞 |
| DELETE | `/guides/{id}/like` | 登录 | 取消点赞 |
| GET | `/guides/{guideId}/comments` | 公开 | 评论列表 |
| POST | `/guides/{guideId}/comments` | 登录 | 发表评论 |
| DELETE | `/guides/{guideId}/comments/{commentId}` | 登录 | 删除评论 |
| POST | `/uploads/image` | 登录 | 上传单张图片 |
| POST | `/uploads/images` | 登录 | 批量上传 |
| DELETE | `/uploads/{filename}` | 登录 | 删除图片 |
| GET | `/collections` | 登录 | 我的收藏列表 |
| GET | `/collections/status` | 登录 | 查询收藏状态，参数 `targetType`、`targetId` |
| POST | `/guides/{guideId}/collect` | 登录 | 收藏攻略 |
| DELETE | `/guides/{guideId}/collect` | 登录 | 取消收藏攻略 |
| DELETE | `/collections/{targetType}/{targetId}` | 登录 | 取消收藏任意目标 |
| POST | `/admin/destinations` | 运营 | 创建目的地 |
| PUT | `/admin/destinations/{id}` | 运营 | 更新目的地 |
| DELETE | `/admin/destinations/{id}` | 运营 | 删除目的地 |
| POST | `/admin/attractions` | 运营 | 创建景点 |
| PUT | `/admin/attractions/{id}` | 运营 | 更新景点 |
| DELETE | `/admin/attractions/{id}` | 运营 | 删除景点 |

### 4.3 行程、日程、行程项、分享

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/itineraries` | 登录 | 创建行程 |
| GET | `/itineraries` | 登录 | 我的行程列表 |
| GET | `/itineraries/{id}` | 登录 | 行程详情 |
| PUT | `/itineraries/{id}` | 登录 | 编辑行程 |
| DELETE | `/itineraries/{id}` | 登录 | 删除行程 |
| POST | `/itineraries/{id}/days` | 登录 | 添加日程 |
| PUT | `/itineraries/days/{dayId}` | 登录 | 编辑日程 |
| DELETE | `/itineraries/days/{dayId}` | 登录 | 删除日程 |
| POST | `/itineraries/{id}/days/{dayId}/items` | 登录 | 添加行程项 |
| PUT | `/itineraries/items/{itemId}` | 登录 | 编辑行程项 |
| DELETE | `/itineraries/items/{itemId}` | 登录 | 删除行程项 |
| POST | `/itineraries/days/{dayId}/items/reorder` | 登录 | 批量排序行程项 |
| POST | `/itineraries/{id}/collection?add=true` | 登录 | 收藏行程 |
| POST | `/itineraries/{id}/collection?add=false` | 登录 | 取消收藏行程 |
| POST | `/itineraries/{id}/share` | 登录 | 生成分享链接 |
| GET | `/itineraries/share/{shareCode}` | 公开 | 查看分享行程 |
| DELETE | `/itineraries/{id}/share` | 登录 | 取消分享 |

### 4.4 盲盒：模板、订单、支付、开盒、结果、秒杀、合规

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/blind-box` | 公开 | 盲盒模板列表 |
| GET | `/blind-box/{id}` | 公开 | 模板详情 |
| GET | `/blind-box/my` | 登录 | 我的盲盒列表 |
| POST | `/blind-box/orders` | 登录 | 创建订单 |
| GET | `/blind-box/orders` | 登录 | 我的订单列表 |
| GET | `/blind-box/orders/{orderNo}` | 登录 | 订单详情 |
| POST | `/blind-box/orders/{orderNo}/cancel` | 登录 | 取消订单 |
| POST | `/payments/blind-box/callback` | 公开 | 模拟支付回调 |
| POST | `/blind-box/orders/{orderNo}/open` | 登录 | 开盒（异步生成方案） |
| GET | `/blind-box/orders/{orderNo}/result` | 登录 | 结果详情 |
| POST | `/blind-box/orders/{orderNo}/share-result` | 登录 | 生成结果分享链接 |
| GET | `/blind-box/share/{shareCode}` | 公开 | 查看分享结果 |
| POST | `/blind-box/flash-sale` | 登录 | 限定盲盒抢购 |
| GET | `/blind-box/flash-sale/{orderNo}/status` | 登录 | 查询秒杀建单状态 |
| GET | `/blind-box/{id}/probabilities` | 公开 | 概率公示 |
| POST | `/blind-box/orders/{orderNo}/import-itinerary` | 登录 | 开盒结果导入行程（幂等） |
| GET | `/blind-box/draw-records` | 登录 | 我的抽取记录 |

### 4.5 管理端

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/admin/users` | 管理员 | 用户列表 |
| GET | `/admin/users/{id}` | 管理员 | 用户详情 |
| POST | `/admin/users/{id}/ban` | 管理员 | 封禁用户 |
| POST | `/admin/users/{id}/unban` | 管理员 | 解封用户 |
| GET | `/admin/guides` | 运营 | 攻略审核列表 |
| GET | `/admin/guides/{id}` | 运营 | 审核详情 |
| POST | `/admin/guides/{id}/approve` | 运营 | 通过审核 |
| POST | `/admin/guides/{id}/reject` | 运营 | 驳回审核 |
| GET | `/admin/comments` | 运营 | 评论列表 |
| DELETE | `/admin/comments/{id}` | 运营 | 删除评论 |
| GET | `/admin/content/stats` | 运营 | 内容数据 |
| GET | `/admin/blind-box/templates` | 管理员 | 模板列表 |
| POST | `/admin/blind-box/templates` | 管理员 | 创建模板 |
| PUT | `/admin/blind-box/templates/{id}` | 管理员 | 更新模板 |
| DELETE | `/admin/blind-box/templates/{id}` | 管理员 | 删除模板 |
| GET | `/admin/blind-box/orders` | 管理员 | 全部订单列表 |
| GET | `/admin/blind-box/orders/{orderNo}` | 管理员 | 订单详情 |
| POST | `/admin/blind-box/orders/{orderNo}/refund` | 管理员 | 订单退款 |
| POST | `/admin/blind-box/orders/{orderNo}/retry-open` | 管理员 | 异常重试开盒 |
| GET | `/admin/blind-box/draw-records` | 管理员 | 抽取记录查询 |
| GET | `/admin/banners` | 管理员 | 轮播图列表 |
| POST | `/admin/banners` | 管理员 | 创建轮播图 |
| GET | `/banners` | 公开 | 公开端轮播图 |
| GET | `/admin/recommendations` | 管理员 | 推荐位列表 |
| POST | `/admin/recommendations` | 管理员 | 创建推荐位 |
| GET | `/recommendations` | 公开 | 公开端推荐位 |
| GET | `/admin/dashboard` | 管理员 | 平台数据概览 |

## 5. 未实现的接口

仅在本地设计文档中描述、当前没有实现：`POST /blind-box/analyze-image`（AI 图片分析）、`GET /blind-box/orders/{orderNo}/result-image`（结果图下载）。契约里不预留路径，实现时再补进本文件。

## 6. 明细在哪

| 想看什么 | 本地文件 |
|---|---|
| 认证与用户接口明细 | `docs/api/user.md` |
| 内容、评论、上传、收藏明细 | `docs/api/content.md` |
| 行程、日程、分享明细 | `docs/api/itinerary.md` |
| 盲盒、支付、开盒、秒杀、合规明细 | `docs/api/blindbox.md` |
| 管理端明细 | `docs/api/admin.md` |
| 模块实现与数据模型 | `docs/modules/`、`docs/architecture.md` |

本文件入库；`docs/` 全部本地，不提交。
