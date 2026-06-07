# Ctrip 项目开发计划

> 基于 `docs/ctrip_modules.md` 文档，按模块依赖关系分阶段推进。
> 整体策略：先完成后端核心模块，再逐步集成前端。
> 目标：两周内完成 MVP（内容社区 + 行程规划 + 盲盒核心流程）。

---

## Phase 1：用户模块 ✅ 已完成

### 已实现功能

| 功能 | 接口 | 状态 |
|------|------|------|
| 手机号注册 | `POST /api/v1/auth/register/phone` | ✅ |
| 邮箱注册 | `POST /api/v1/auth/register/email` | ✅ |
| 登录 | `POST /api/v1/auth/login` | ✅ |
| 令牌刷新 | `POST /api/v1/auth/refresh` | ✅ |
| 登出 | `POST /api/v1/auth/logout` | ✅ |
| 忘记密码 | `POST /api/v1/auth/password/forgot` | ✅ |
| 重置密码 | `POST /api/v1/auth/password/reset` | ✅ |
| 发送短信验证码 | `POST /api/v1/auth/sms/send` | ✅ |
| 查看个人资料 | `GET /api/v1/users/me` | ✅ |
| 更新个人资料 | `PUT /api/v1/users/me` | ✅ |
| 修改密码 | `PUT /api/v1/users/me/password` | ✅ |
| 更新头像 | `PUT /api/v1/users/me/avatar` | ✅ |

### 技术要点

- **认证**：JWT (HS256)，Access Token 15分钟 + Refresh Token 7天轮换
- **安全**：BCrypt 密码加密、SHA-256 令牌哈希、防枚举攻击、Token 轮换防重放
- **架构**：Spring Security + MyBatis Plus + MySQL
- **数据库**：`users`、`refresh_tokens`、`password_reset_tokens`（SQL 见 `docs/SQL/ctrip_user.sql`）
- **测试**：JWT 过滤器单元测试、Service 层测试

### 待补充

- [ ] 邮箱验证功能（发送验证邮件、验证链接）
- [ ] 第三方登录（微信/Google）
- [ ] 短信服务真实对接（当前 log.debug 输出验证码）

---

## Phase 2：内容与目的地模块

**前置依赖**：Phase 1（用户模块）✅

### 2.1 数据库设计

新增表（参考 `ctrip_modules.md` 第七章）：

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `destination` | 目的地 | id, name, country, province, description, best_season, cover_url, image_urls(JSON), created_at |
| `attraction` | 景点 | id, destination_id(FK), name, description, location, ticket_price, cover_url, image_urls(JSON), created_at |
| `guide` | 攻略内容 | id, author_id(FK→users), title, content, destination_id, cover_url, image_urls(JSON), status(DRAFT/PUBLISHED/REJECTED), view_count, like_count, created_at, updated_at |
| `comment` | 评论 | id, guide_id(FK), user_id(FK), parent_id(自关联, 支持回复), content, like_count, created_at |
| `collection` | 收藏 | id, user_id(FK), target_type(guide/destination/itinerary), target_id, created_at |

**图片字段说明**：
- `cover_url`：封面图 URL，用于列表卡片展示（单张）
- `image_urls`：JSON 数组 `["url1", "url2", ...]`，存储内容图片，支持多图浏览（如景点相册、攻略配图、目的地图集）
- 上传接口统一返回 `{ "url": "..." }`，业务实体创建时将 URL 写入对应字段

### 2.2 目的地管理

| 任务 | 接口 | 说明 |
|------|------|------|
| 目的地列表 | `GET /api/v1/destinations` | 分页、按省份/国家筛选、关键词搜索 |
| 目的地详情 | `GET /api/v1/destinations/{id}` | 含封面图、多图集、关联景点列表 |
| 目的地 CRUD | `GET/POST/PUT/DELETE /api/v1/admin/destinations` | 仅后台管理可写，支持 cover_url + image_urls |

### 2.3 景点管理

| 任务 | 接口 | 说明 |
|------|------|------|
| 景点列表 | `GET /api/v1/attractions` | 按目的地筛选、关键词搜索、分页 |
| 景点详情 | `GET /api/v1/attractions/{id}` | 含封面图、多图集、关联攻略 |
| 景点 CRUD | `GET/POST/PUT/DELETE /api/v1/admin/attractions` | 仅后台管理可写，支持 cover_url + image_urls |

### 2.4 攻略社区

| 任务 | 接口 | 说明 |
|------|------|------|
| 攻略发布 | `POST /api/v1/guides` | 需 JWT，关联作者，支持草稿/发布，含 cover_url + image_urls |
| 攻略列表 | `GET /api/v1/guides` | 分页、按目的地/标签/作者筛选、按时间/热度排序，返回封面图 |
| 攻略详情 | `GET /api/v1/guides/{id}` | 含作者信息、封面图、内容图集、评论列表、是否已收藏 |
| 攻略编辑/删除 | `PUT/DELETE /api/v1/guides/{id}` | 仅作者或管理员，可更新图片列表 |
| 攻略点赞 | `POST/DELETE /api/v1/guides/{id}/like` | 幂等操作 |
| 评论列表 | `GET /api/v1/guides/{id}/comments` | 支持树形结构（parent_id） |
| 发表评论 | `POST /api/v1/guides/{id}/comments` | 需 JWT，支持回复（parent_id） |
| 删除评论 | `DELETE /api/v1/comments/{id}` | 仅评论作者或管理员 |
| 收藏攻略 | `POST/DELETE /api/v1/guides/{id}/collection` | 幂等操作 |

### 2.5 图片上传与管理

| 任务 | 接口 | 说明 |
|------|------|------|
| 上传图片 | `POST /api/v1/uploads/image` | 单张上传，返回 `{ "url": "..." }`，MVP 本地存储 |
| 批量上传 | `POST /api/v1/uploads/images` | 多图上传，返回 `{ "urls": ["url1", "url2", ...] }` |
| 删除图片 | `DELETE /api/v1/uploads/{filename}` | 删除本地文件，可选接口 |

### 2.6 技术要点

- **分页**：MyBatis Plus PaginationInterceptor
- **搜索**：LIKE 模糊匹配（MVP），后续可接入 Elasticsearch
- **图片存储**：MVP 阶段使用 `ImageStorageService` 接口抽象，`LocalStorageServiceImpl` 实现（`uploads/images/` 目录，按日期分目录如 `2026/05/18/xxx.jpg`），后续切换 `OssStorageServiceImpl` 即可
- **图片关联**：业务表使用 `cover_url`（单张封面）+ `image_urls`（JSON 数组存多图），方案 A，不建独立图片表
- **权限**：攻略仅作者可编辑/删除，评论同理
- **排序**：按创建时间/浏览量/点赞数排序
- **图片限制**：单张最大 5MB，支持 jpg/png/webp 格式，后端校验文件头防止伪装

### 2.7 验收标准

- [ ] 目的地/景点列表分页正常，筛选条件生效
- [ ] 目的地/景点详情返回封面图 + 多图集
- [ ] 攻略发布、编辑、删除流程完整，图片关联正确
- [ ] 评论支持回复（树形展示）
- [ ] 点赞/收藏幂等操作正常
- [ ] 图片上传返回可访问 URL，删除图片清理文件
- [ ] 所有接口有单元测试覆盖

---

## Phase 3：行程规划模块

**前置依赖**：Phase 1 ✅ + Phase 2 部分（目的地/景点数据）

### 3.1 数据库设计

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `itinerary` | 行程主表 | id, user_id(FK→users), title, destination_id, start_date, end_date, status(DRAFT/PUBLISHED/ARCHIVED), view_count, like_count, created_at, updated_at |
| `itinerary_day` | 行程日程 | id, itinerary_id(FK), day_number, title, sort_order |
| `itinerary_item` | 行程明细 | id, itinerary_day_id(FK), type(HOTEL/ATTRACTION/TRANSPORT/FOOD/ACTIVITY), name, location, time_slot, description, sort_order |

### 3.2 行程管理

| 任务 | 接口 | 说明 |
|------|------|------|
| 创建行程 | `POST /api/v1/itineraries` | 需 JWT，关联目的地、起止日期 |
| 我的行程 | `GET /api/v1/itineraries` | 仅当前用户，按时间排序 |
| 行程详情 | `GET /api/v1/itineraries/{id}` | 含完整日程、行程项 |
| 编辑行程 | `PUT /api/v1/itineraries/{id}` | 仅创建者 |
| 删除行程 | `DELETE /api/v1/itineraries/{id}` | 仅创建者，级联删除日程/行程项 |
| 收藏行程 | `POST/DELETE /api/v1/itineraries/{id}/collection` | 幂等操作 |

### 3.3 日程管理

| 任务 | 接口 | 说明 |
|------|------|------|
| 添加日程 | `POST /api/v1/itineraries/{id}/days` | 指定 day_number |
| 编辑日程 | `PUT /api/v1/days/{dayId}` | 修改标题、排序 |
| 删除日程 | `DELETE /api/v1/days/{dayId}` | 级联删除行程项 |
| 添加行程项 | `POST /api/v1/days/{dayId}/items` | 指定类型、名称、位置、时间 |
| 编辑行程项 | `PUT /api/v1/items/{id}` | 修改详情 |
| 删除行程项 | `DELETE /api/v1/items/{id}` | |
| 批量排序 | `PUT /api/v1/days/{dayId}/items/reorder` | 接收 item_id 列表，批量更新 sort_order |

### 3.4 分享功能

| 任务 | 接口 | 说明 |
|------|------|------|
| 生成分享链接 | `POST /api/v1/itineraries/{id}/share` | 生成只读公开链接（短码） |
| 查看分享行程 | `GET /api/v1/share/{shareCode}` | 无需登录，只读 |
| 取消分享 | `DELETE /api/v1/itineraries/{id}/share` | 使分享链接失效 |

### 3.5 技术要点

- **级联删除**：MyBatis Plus 不原生支持，需在 Service 层手动处理
- **分享链接**：生成随机短码（6位 Base62），存 `itinerary.share_code`
- **权限**：仅创建者可编辑/删除，分享链接只读
- **事务**：行程创建+日程添加需在同一事务内

### 3.6 验收标准

- [ ] 行程 CRUD 完整，日程/行程项可正常增删改
- [ ] 批量排序接口正常工作
- [ ] 分享链接可访问，取消后失效
- [ ] 删除行程时级联删除日程和行程项
- [ ] 所有接口有单元测试覆盖

---

## Phase 4：盲盒模块（核心）

**前置依赖**：Phase 1 ✅ + Phase 2（目的地数据）+ Phase 3（行程导入）

### 4.1 数据库设计

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `blind_box_template` | 盲盒模板 | id, name, type(DAILY/LIMITED), price, stock, rule_config(JSON), status(ACTIVE/INACTIVE), created_at |
| `blind_box_order` | 盲盒订单 | id, user_id(FK→users), template_id(FK), order_no, status(PENDING/PAID/OPENED/REFUNDED/CANCELLED), pay_amount, pay_method, pay_time, expire_at, created_at |
| `blind_box_result` | 盲盒结果 | id, order_id(FK), destination, theme, result_text(LONGTEXT JSON), result_image_url(OSS), share_code(短码), share_expires_at, opened_at |
| `blind_box_preference` | 盲盒预选参数 | id, order_id(FK), departure_city, budget_level(ECONOMY/STANDARD/LUXURY), theme, image_tags(JSON), created_at |

### 4.2 盲盒模板管理

> **盲盒类型说明**：
> - **日常盲盒（DAILY）**：不限量，用户可随时购买，`stock` 字段标记为 -1 表示无限
> - **限定盲盒（LIMITED）**：限时限量，需抢购，`stock` 为具体数值，配合 Redis 扣减库存

| 任务 | 接口 | 说明 |
|------|------|------|
| 盲盒列表 | `GET /api/v1/blind-box` | 前台展示，含价格/库存/活动标签 |
| 模板详情 | `GET /api/v1/blind-box/{id}` | 规则说明、往期示例 |
| 模板 CRUD | `GET/POST/PUT/DELETE /api/v1/admin/blind-box/templates` | 仅后台管理 |

### 4.3 购买与支付

| 任务 | 接口 | 说明 |
|------|------|------|
| 创建订单 | `POST /api/v1/blind-box/orders` | 提交预选参数，15分钟支付超时 |
| 订单查询 | `GET /api/v1/blind-box/orders/{orderNo}` | |
| 我的订单 | `GET /api/v1/blind-box/orders` | 当前用户订单列表 |
| 支付回调 | `POST /api/v1/payments/callback` | 微信/支付宝回调（MVP 阶段模拟） |
| 取消订单 | `POST /api/v1/blind-box/orders/{orderNo}/cancel` | 支付前可取消，恢复库存 |

### 4.4 AI 盲盒生成

#### 预选参数（图片分析暂不实现）

> **AI 图片分析暂不实现**：AI 处理图片提取特征信息的接口尚未实现，盲盒生成中暂不勾选上传图片 AI 分析功能。
> 以下保留设计文档，后续迭代接入。

盲盒生成前用户可提交一组预选参数（`ctrip_modules.md` 3.2-3.3）：

```
设定出发地/时间范围 → 选择预算区间 → 设定旅行主题(可选) → 上传参考图片(可选，暂不实现)
```

| 任务 | 接口 | 说明 |
|------|------|------|
| ~~图片分析~~ | ~~`POST /api/v1/blind-box/analyze-image`~~ | **暂不实现**：上传参考图片，AI 分析特征标签 |
| 创建订单 | `POST /api/v1/blind-box/orders` | 提交预选参数（出发地、预算、主题等），15分钟支付超时 |
| 开盒 | `POST /api/v1/blind-box/orders/{orderNo}/open` | 解锁方案、返回结果 |

**图片分析流程**（与通用图片上传不同，**暂不实现**）：

```
用户上传图片 → 读到内存 → 调用 AI API 分析 → 返回特征标签 → 丢弃图片（不持久化）
```

- **不经过通用上传接口**：图片是临时性的，分析完即丢弃，不存持久存储
- **返回示例**：`{ "tags": ["beach", "tropical", "ocean"], "confidence": 0.92 }`
- 用户可在创建订单时将 `imageTags` 与出发地、预算等一并提交

**订单创建示例**：
```json
POST /api/v1/blind-box/orders
{
  "templateId": 1,
  "departureCity": "上海",
  "budgetLevel": "STANDARD",
  "theme": "beach"
}
```

预选参数存入 `blind_box_preference` 表，其中 `image_tags JSON` 字段（**暂不实现**：AI 图片分析尚未就绪）。

**MVP 阶段 AI 实现策略**：
- 图片识别：MVP 阶段可 mock 返回预设标签（如 `["mountain", "nature"]`），后续对接通义千问/文心一言等大模型 API
- 方案生成：基于模板规则 + 目的地数据库随机选取 + 预设规则生成（非纯 AI）
- 后续迭代：接入完整 AI 生成链路
- 封装 `AiService` 接口：`List<String> analyzeImage(InputStream image)`，MVP 返回 mock 数据，后续替换为真实 API 调用

### 4.5 盲盒结果与行程关联

| 任务 | 接口 | 说明 |
|------|------|------|
| 我的盲盒 | `GET /api/v1/blind-box/my` | 购买/开盒记录 |
| 结果详情 | `GET /api/v1/blind-box/orders/{orderNo}/result` | 完整旅行方案（JSON 格式：目的地/行程/酒店/交通/预算） |
| 导入行程 | `POST /api/v1/blind-box/orders/{orderNo}/to-itinerary` | 一键转为行程（复制 itinerary_data 到行程表） |
| ~~下载结果图~~ | ~~`GET /api/v1/blind-box/orders/{orderNo}/result-image`~~ | **暂不实现**：返回 AI 生成的可视化行程单图片 |
| 生成分享链接 | `POST /api/v1/blind-box/orders/{orderNo}/share-result` | 生成只读分享链接（结果图暂不展示），返回短码 |
| 查看分享结果 | `GET /api/v1/blind-box/share/{shareCode}` | 无需登录，只读查看行程概览（结果图暂不展示） |

### 4.6 技术要点

- **库存扣减**：日常盲盒无需库存（`stock = -1`），限定盲盒使用 Redis 扣减库存（`stock > 0`），MVP 阶段数据库行锁（`SELECT ... FOR UPDATE`）
- **订单超时**：定时任务扫描过期订单，自动取消 + 恢复库存
- **幂等性**：支付回调需幂等处理，防止重复回调
- **AI 集成**：封装 `AiService` 接口（`List<String> analyzeImage(InputStream image)`），MVP 返回 mock 数据，后续对接真实大模型 API（**图片分析功能暂不实现**）
- **异步处理**：开盒生成可异步（MVP 阶段同步即可，后续接入 RabbitMQ）
- **预选参数**：~~`POST /api/v1/blind-box/analyze-image` 图片分析后不持久化图片，仅返回特征标签存入 `blind_box_preference.image_tags`~~（**暂不实现**）；创建订单时与出发地、预算等一并提交
- **盲盒方案生成**：基于 `blind_box_preference` 中的标签/主题/预算等参数，从目的地数据库按权重随机选取 + 预设规则组装行程
- **结果图生成**：~~MVP 阶段使用预设模板生成静态图片（如 Java 绘图 / HTML 转图片），存入 `result_image_url`~~（**暂不实现**：AI 生成结果图功能尚未就绪）；后续接入 AI 生成
- **结果图下载**：~~`GET /api/v1/blind-box/orders/{orderNo}/result-image` 返回图片二进制流~~（**暂不实现**）
- **结果分享**：生成 6 位 Base62 短码存 `share_code`，分享链接只读展示结果图 + 行程概览（**结果图暂不展示**），可设置过期时间（`share_expires_at`）

### 4.7 验收标准

- [ ] 盲盒列表展示正常，库存/价格正确
- [ ] ~~图片分析接口返回特征标签（MVP mock 即可），不持久化图片~~（**暂不实现**）
- [ ] 创建订单时预选参数正确存入 `blind_box_preference` 表
- [ ] 创建订单 → 模拟支付 → 支付成功流程完整
- [ ] 订单超时自动取消
- [ ] 开盒返回完整旅行方案（目的地 + 酒店 + 行程数据 + 预算）
- [ ] 盲盒结果可一键导入行程
- [ ] ~~结果图可下载（PNG/JPEG），浏览器触发文件下载~~（**暂不实现**）
- [ ] 结果分享链接可访问，取消/过期后失效（结果图暂不展示）
- [ ] 库存扣减/恢复正确，无超卖
- [ ] 所有接口有单元测试覆盖

---

## Phase 5：后台管理模块（MVP 后置）

**前置依赖**：Phase 1-4 完成

> MVP 阶段不需要完整后台管理，可通过数据库直接操作或简易接口代替。

| 任务 | 说明 |
|------|------|
| 管理端认证 | 基于角色（ADMIN/USER）的权限控制，需在 users 表增加 role 字段 |
| 用户管理 | 用户列表、封禁/解封、标签 |
| 内容审核 | 攻略审核、评论管理 |
| 盲盒管理 | 模板配置、库存管理、活动规则 |
| 订单管理 | 订单查询、退款处理 |

---

## Phase 6：运营与优化（MVP 后置）

| 任务 | 说明 |
|------|------|
| 消息通知 | 站内消息、短信推送、出行提醒 |
| 积分体系 | 用户行为记录、积分计算、兑换 |
| 分享裂变 | 分享链接追踪、返利机制 |
| 性能优化 | Redis 缓存热点数据、数据库索引优化 |
| 安全加固 | 接口限流、SQL注入/XSS防护 |

---

## 技术架构概览

### 项目结构

```
src/main/java/com/ctrip/
├── config/          # 配置类（Security、MyBatis、JWT 等）
├── common/          # 公共组件（异常处理、统一响应、工具类）
├── user/            # 用户模块（Phase 1 ✅）
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   ├── dto/
│   └── security/
├── content/         # 内容模块（Phase 2）
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── itinerary/       # 行程模块（Phase 3）
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── blindbox/        # 盲盒模块（Phase 4）
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   ├── dto/
│   └── ai/          # AI 服务接口
└── upload/          # 文件上传（Phase 2）
    ├── controller/
    └── service/
```

### 技术栈

| 层级 | 技术选型 | 用途 |
|------|---------|------|
| 后端框架 | Spring Boot 3.5.13 | RESTful API、业务逻辑 |
| 身份认证 | JWT (HS256) | 无状态认证、Token 管理 |
| ORM | MyBatis Plus | 数据访问 |
| 数据库 | MySQL 8.0 | 核心数据存储 |
| 缓存 | Redis | 热点数据缓存、会话管理（后续） |
| 对象存储 | 本地存储 / 阿里云 OSS | 图片上传（MVP 本地，后续 OSS） |
| 消息队列 | RabbitMQ | 异步消息、订单状态同步（后续） |
| AI 服务 | 大模型 API | 图片识别、盲盒方案生成 |
| 构建工具 | Maven | 项目构建 |
| 测试 | JUnit 5 + Mockito | 单元测试 |

### 统一响应格式

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

---

## 开发排期（两周 MVP 目标）

> 整体策略：两周完成核心功能 MVP，Phase 5/6 后置。
> 假设：全职开发，每天 8 小时有效编码时间。

### 第一周：内容 + 行程（约 7 天）

| 阶段 | 时间 | 任务 | 里程碑 |
|------|------|------|--------|
| W1-D1~D2 | 2 天 | Phase 2.1-2.3：目的地/景点 CRUD + 列表/详情 API | 目的地/景点数据可查询 |
| W1-D3~D4 | 2 天 | Phase 2.4-2.5：攻略发布/浏览/评论/点赞/收藏 + 图片上传 | 攻略社区可用 |
| W1-D5~D6 | 2 天 | Phase 3.2-3.3：行程 CRUD + 日程管理 + 批量排序 | 行程规划全流程可用 |
| W1-D7 | 1 天 | Phase 3.4 + Phase 2 收尾：行程分享 + 测试修复 | 社区 + 行程完整闭环 |

**第一周验收**：内容社区可浏览发布，行程可创建编辑分享。

### 第二周：盲盒核心流程（约 7 天）

| 阶段 | 时间 | 任务 | 里程碑 |
|------|------|------|--------|
| W2-D1~D2 | 2 天 | Phase 4.2-4.3：盲盒模板 + 购买下单 + 模拟支付 + 订单超时 | 盲盒购买流程可用 |
| W2-D3~D4 | 2 天 | Phase 4.4：AI 图片分析 + 盲盒方案生成 + 开盒流程 | 开盒全流程闭环 |
| W2-D5 | 1 天 | Phase 4.5：我的盲盒 + 导入行程 | 盲盒结果可转行程 |
| W2-D6~D7 | 2 天 | 联调整合 + Bug 修复 + 测试覆盖 | 完整 MVP 可演示 |

**第二周验收**：盲盒购买 → 开盒 → 导入行程 全流程可演示。

### MVP 范围界定

**MVP 包含**：
- ✅ 用户注册/登录/资料管理
- ✅ 目的地/景点浏览
- ✅ 攻略发布/浏览/评论/点赞/收藏
- ✅ 行程创建/编辑/分享
- ✅ 盲盒购买/开盒/导入行程
- ✅ 图片上传（本地存储）

**MVP 不包含**（后置）：
- ❌ 后台管理系统（Phase 5）
- ❌ 真实支付对接（模拟支付代替）
- ❌ 真实 AI 图片识别（mock 返回预设标签，`AiService` 接口留好）
- ❌ Redis 缓存/RabbitMQ 消息队列
- ❌ 阿里云 OSS（本地存储代替）
- ❌ 消息通知/积分体系/分享裂变（Phase 6）
- ❌ 前端 UI（仅提供 API，前端后续开发）

### MVP 之后（Phase 5/6 后置）

| 阶段 | 说明 |
|------|------|
| Phase 5 | 后台管理模块（MVP 阶段用简易接口/数据库直操作代替） |
| Phase 6 | 运营功能（积分、分享裂变、消息通知等） |

> **MVP 总时间**：2 周（约 14 天）
> **目标**：内容社区 + 行程规划 + 盲盒购买/开盒全流程可演示
> **交付物**：完整后端 API + Swagger 文档 + 单元测试覆盖

---

## 风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| AI 服务对接复杂 | 盲盒方案生成延迟 | MVP 使用预设规则 + 随机生成，AI 接口先留空 |
| 支付对接耗时 | 购买流程无法演示 | MVP 使用模拟支付（跳过真实支付回调） |
| 图片存储方案 | 上传功能阻塞 | MVP 使用本地存储，后续切换 OSS |
| 测试覆盖率不足 | 代码质量风险 | 核心业务逻辑必须有测试，工具类可后置 |
| 时间不够 | Phase 4 压缩 | 优先保证盲盒核心流程，分享/收藏等功能可后置 |

---

## 前置准备工作

在开始 Phase 2 之前，需要确认：

- [ ] Phase 1 所有测试通过，无遗留 bug
- [ ] 数据库表结构设计评审完成
- [ ] 统一响应格式、异常处理机制已就绪
- [ ] 图片上传方案确认（MVP 本地存储路径）
- [ ] AI 服务选型确认（哪个大模型 API）

---

## 进度追踪

| Phase | 状态 | 开始时间 | 完成时间 | 备注 |
|-------|------|---------|---------|------|
| Phase 1：用户模块 | ✅ 已完成 | - | 2026-05-17 | 核心功能完整 |
| Phase 2：内容模块 | ⏳ 待开始 | - | - | |
| Phase 3：行程模块 | ⏳ 待开始 | - | - | |
| Phase 4：盲盒模块 | ⏳ 待开始 | - | - | |
| Phase 5：后台管理 | ⏳ 后置 | - | - | MVP 后 |
| Phase 6：运营优化 | ⏳ 后置 | - | - | MVP 后 |
