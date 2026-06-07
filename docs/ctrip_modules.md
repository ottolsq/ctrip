## 一、项目定位

- **类型**：轻量级旅游 Web 平台
- **核心**：攻略内容(轻社区) + 行程规划 + **旅游盲盒**
- **目标用户**：年轻人、学生、自由行爱好者、性价比用户
- **业务模式**：内容种草 → 行程规划 → 盲盒玩法 → 关联预订
- **迭代策略**：MVP 极简上线，先国内后海外
- **三大核心：内容社区、行程规划、旅游盲盒**

------

## 二、前台功能模块

### 1. 首页模块

- 全局搜索：目的地 / 景点 / 攻略搜索 / **AI智能搜索**
- 轮播 Banner、热门目的地推荐
- **盲盒专区（固定醒目入口）**
- 特价机票、特价酒店推荐
- 攻略精选、热门行程推荐

### 2. 目的地 & 攻略模块

- 目的地基础信息：景点、美食、玩法、最佳旅行时间
- UGC 攻略（用户生成内容：游客、政府推广部门、旅游企业上传的旅游攻略，含文本/图片/视频数据，样本量大、时效性强，但数据质量需审核）
- ~~游记、点评、收藏~~
- 按行程天数分类：1-3 天、3-5 天、5 天以上
- 景点清单、旅行避坑指南
- 轻社区：旅游攻略、用户点评、用户分享

### 3. 行程规划模块

- 自由创建行程：添加天数、景点、交通、住宿
- 拖拽排序、导出、分享
- 目的地智能路线推荐
- 一键关联机票 / 酒店预订

### 4. 旅游盲盒模块（核心功能）

- **盲盒类型**：
  - **日常盲盒**：不限量，用户可随时购买，价格稳定，适合日常消费
  - **限定盲盒**：限时限量，用户需抢购获得，可设置特殊主题/折扣/活动标签
- **核心功能**：购买、开盒、分享、规则查看、退款说明

### 5. 用户中心模块

- 注册 / 登录（手机号 + 验证码 / 第三方登录）
- 个人资料管理
- 我的收藏：攻略、行程、盲盒
- 我的订单：机票、酒店、盲盒订单
- **我的盲盒**：购买记录、开盒记录、已兑换行程
- 我的发布、消息通知

### 6. 通用工具

- 全局搜索
- 评价系统
- 客服 / 帮助中心

-----------

## 三、后台管理模块

1. **用户管理**：用户列表、信息查看、状态管理、标签分组
2. **内容管理**：目的地、攻略、景点维护，内容审核~~、推荐位管理~~
3. **产品管理**：盲盒类型配置（日常/限定）、价格策略、库存管理（限定盲盒限量）
4. **订单管理**：订单查询、状态同步、退款处理
5. **盲盒管理**：盲盒模板（日常/限定）、概率配置、活动规则、数据看板、限定盲盒抢购管理
6. **运营管理**：轮播图、弹窗、Banner 管理，数据概览

---

## 四、技术栈与中间件

| 层级 | 技术选型 | 用途 |
|------|---------|------|
| 后端框架 | Spring Boot | RESTful API、业务逻辑 |
| 身份认证 | JWT | 无状态认证、Token 管理 |
| 数据库 | MySQL | 用户、订单、内容等核心数据存储 |
| 缓存 / 分布式锁 | Redis | 热点数据缓存、会话管理、盲盒库存扣减 |
| 对象存储 | 阿里云 OSS | 用户上传图片、攻略封面、盲盒结果图 |
| 消息队列 | RabbitMQ / Kafka | 异步消息、订单状态同步、开盒事件处理 |
| 前端框架 | UniApp | PC + 移动端响应式 |
| AI 服务 | 大模型 API | 智能搜索、图片识别、盲盒路线生成 |

------

## 五、核心接口

| 模块 | 接口 |
|------|------|
| 用户 | 注册、登录、Token 刷新、个人资料 CRUD |
| 内容 | 攻略列表/详情、目的地信息、景点查询、评论/收藏 |
| 行程 | 行程 CRUD、景点/交通/住宿关联、分享/导出 |
| 盲盒 | 盲盒列表、创建订单、支付回调、开盒逻辑、记录查询 |
| 订单 | 订单创建、状态查询、支付、退款 |
| 运营 | 轮播图、推荐位、活动配置、消息推送 |

---

## 六、核心业务流程

### 1. 内容种草流程

用户通过攻略内容建立旅行意向，为后续的行程规划和盲盒购买做铺垫。

```
浏览首页/搜索 → 查看攻略详情 → 收藏/点赞 → 创建行程 → 关联目的地
```

**步骤说明**：

| 步骤 | 动作 | 涉及模块 | 数据流转 |
|------|------|---------|---------|
| 1 | 用户在首页或搜索入口查找感兴趣的内容 | 首页、搜索 | 搜索关键词 → 返回攻略/目的地列表 |
| 2 | 浏览攻略详情，查看景点、美食、路线推荐 | 目的地 & 攻略 | 加载攻略内容、图片、评论 |
| 3 | 收藏攻略或标记感兴趣的内容 | 用户中心 | 用户 ID + 内容 ID → 收藏表 |
| 4 | 从攻略一键创建行程草案 | 行程规划 | 攻略中的景点 → 行程天数/景点列表 |
| 5 | 编辑完善行程细节（交通、住宿、时间） | 行程规划 | 用户输入 → 行程数据持久化 |

### 2. 行程规划流程

用户在明确旅行意向后，通过行程规划工具制定详细的出行计划。

```
创建行程 → 添加天数 → 添加景点/交通/住宿 → 拖拽排序 → 保存/分享/导出
```

**步骤说明**：

| 步骤 | 动作 | 涉及模块 | 数据流转 |
|------|------|---------|---------|
| 1 | 选择目的地、出行日期、天数创建行程 | 行程规划 | 用户输入 → 行程骨架 |
| 2 | 逐天添加景点、餐厅、活动 | 行程规划 + 目的地 | 景点数据 → 行程日程 |
| 3 | 添加交通方式（航班/火车/自驾） | 行程规划 | 交通信息 → 行程关联 |
| 4 | 添加住宿（酒店/民宿） | 行程规划 | 酒店信息 → 行程关联 |
| 5 | 拖拽调整顺序、优化路线 | 行程规划 | 前端拖拽 → 后端保存排序 |
| 6 | 保存、分享链接或导出 PDF/图片 | 行程规划 | 行程数据 → 生成分享链接/文件 |

### 3. 盲盒旅游流程（核心）

盲盒是本平台的核心差异化功能，通过 AI + 随机性为用户生成独特的旅行体验。

#### ~~3.1 盲盒浏览与选购~~

```
进入盲盒专区 → 浏览盲盒类型 → 查看规则/价格 → 选择盲盒类型
```

| ~~步骤~~ | ~~动作~~ | ~~说明~~ |
|------|------|------|
| ~~1~~ | ~~首页盲盒入口或底部导航进入盲盒专区~~ | ~~展示所有可选盲盒类型~~ |
| ~~2~~ | ~~浏览各类型盲盒卡片（机票/目的地/酒店/综合）~~ | ~~显示价格、库存、活动标签~~ |
| ~~3~~ | ~~点击查看规则说明、退款政策、往期示例~~ | ~~降低用户决策门槛~~ |
| ~~4~~ | ~~选择目标盲盒类型，进入预选页面~~ | ~~准备进入个性化配置~~ |

#### 3.2 预选与主题设定

```
设定出发地/时间范围 → 选择预算区间 → 设定旅行主题（可选）→ 上传参考图片（可选，暂不实现）
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 选择出发城市、可出行的日期范围 | 缩小随机范围，保证可行性 |
| 2 | 选择预算区间（经济/标准/豪华） | 影响盲盒内酒店、交通等级 |
| 3 | 设定旅行主题（美食/海滨/古镇/滑雪/亲子等，可选） | AI 据此推荐匹配的目的地 |
| 4 | ~~上传参考图片（可选，AI 识别风格特征）~~ | **暂不实现**：AI 图片分析接口尚未就绪 |

#### 3.3 AI 图片识别（可选步骤，暂不实现）

> **本功能暂不实现**：AI 处理图片提取特征信息的接口尚未实现，后续迭代接入。

```
上传图片 → AI 分析图片特征 → 匹配目的地/路线特征 → 返回推荐标签
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 用户上传一张或多张参考图片 | 支持 jpg/png，最大 5MB（**暂不实现**） |
| 2 | 后端调用 AI 服务分析图片内容 | 识别场景类型：海滨/雪山/城市/古镇等（**暂不实现**） |
| 3 | AI 返回特征标签及置信度 | 如：`{ "type": "beach", "confidence": 0.92 }`（**暂不实现**） |
| 4 | 特征标签加入盲盒生成参数 | 影响最终目的地的随机权重（**暂不实现**） |

#### 3.4 盲盒生成与支付

```
确认预选参数 → 生成盲盒方案 → 展示预览（隐藏具体目的地）→ 确认支付 → 订单创建
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 汇总用户所有预选参数，提交生成请求 | 后端接收完整配置 |
| 2 | AI 综合参数生成盲盒方案（目的地+酒店+路线） | 方案生成后暂不展示给用户 |
| 3 | 展示盲盒预览页（价格、天数、主题标签，隐藏具体信息） | 营造期待感 |
| 4 | 用户确认并支付 | 调用支付接口 |
| 5 | 支付成功后创建订单，锁定盲盒方案 | 订单状态：待开盒 |
| 6 | Redis 扣减库存（限定盲盒），MQ 异步记录开盒事件 | 限定盲盒需保证高并发下的数据一致性 |

#### 3.5 开盒与结果展示

```
进入我的盲盒 → 点击开盒 → 动画展示 → 展示完整旅行方案 → 生成结果图
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 用户在「我的盲盒」中查看待开盒记录 | 显示开盒按钮 |
| 2 | 点击开盒，播放开盒动画 | 前端动画 + 后端解锁数据 |
| 3 | 展示完整方案：目的地、酒店、景点、路线、预算 | 核心信息全部呈现 |
| 4 | ~~AI 生成旅行路线图像（可视化行程单）~~ | 包含地图路线、景点标记、时间线（**暂不实现**） |
| 5 | 用户可保存结果、分享至社交平台 | 图片保存至 OSS，生成分享链接 |

#### 3.6 行程关联与预订

```
一键导入行程 → 补充细节 → 关联机票/酒店预订 → 完成出行准备
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 用户将盲盒结果一键导入行程规划 | 盲盒数据 → 行程数据 |
| 2 | 在行程中补充/调整细节（餐饮、活动等） | 个性化完善 |
| 3 | 查看盲盒包含的机票/酒店详情 | 航班号、酒店名称、地址 |
| 4 | 确认预订信息，完成出票/入住确认 | 如为综合盲盒则自动完成 |
| 5 | 出行前收到提醒通知 | 短信/站内消息推送 |

### 4. 订单与支付流程

```
创建订单 → 选择支付方式 → 支付 → 回调确认 → 订单状态更新 → 通知用户
```

| 步骤 | 动作 | 说明 |
|------|------|------|
| 1 | 用户提交盲盒购买请求，后端创建待支付订单 | 订单号 + 15 分钟支付超时 |
| 2 | 用户选择支付方式（微信/支付宝） | 跳转支付页面 |
| 3 | 支付完成，第三方回调通知后端 | Webhook 回调 |
| 4 | 后端验证回调签名，更新订单状态为已支付 | 幂等处理，防重复回调 |
| 5 | 异步 MQ 消息触发后续流程（库存扣减、方案生成） | 解耦核心链路 |
| 6 | 推送支付成功通知给用户 | 站内消息 + 短信 |

### 5. 用户成长与运营流程

```
注册 → 首次浏览 → 收藏/互动 → 首次购买盲盒 → 分享 → 复购
```

| 阶段 | 运营策略 | 技术手段 |
|------|---------|---------|
| 新手引导 | 首次注册赠送盲盒优惠券 | 注册事件 → 发券 |
| 内容互动 | 浏览攻略推荐相关盲盒 | 内容标签 → 盲盒推荐 |
| 首次转化 | 限时特惠、学生折扣 | 活动配置 + 身份校验 |
| 分享裂变 | 开盒结果分享返优惠券 | 分享链接追踪 + 返利 |
| 复购激励 | 集卡活动、积分体系 | 用户行为记录 + 积分计算 |

---

## 七、数据库设计概要

### 核心表结构

> **用户模块相关表已实现**，SQL 文件位于 `docs/SQL/ctrip_user.sql`。

#### 用户模块（已实现）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `users` | 用户主表 | id, username, email, phone, password_hash(BCrypt), avatar_url, gender, birthday, real_name, status(UNVERIFIED/ACTIVE/SUSPENDED/DELETED), role(USER/ADMIN/CONTENT_OPERATOR), email_verified, phone_verified, last_login_at, created_at, updated_at |
| `refresh_tokens` | 刷新令牌表（Token轮换） | id, user_id(FK→CASCADE), token_hash(SHA-256), device_info, issued_at, expires_at, revoked, revoked_at |
| `password_reset_tokens` | 密码重置OTP表 | id, user_id(FK→CASCADE), token_hash(SHA-256), channel(EMAIL/SMS), expires_at, used |

**设计要点：**
- 密码使用 BCrypt 加密（强度12），原始密码永不落库
- Refresh Token 只存 SHA-256 哈希，不存原始值，支持 Token 轮换防重放
- 密码重置 OTP 同样哈希存储，一次性使用，10分钟有效期
- 登录时先检查账号状态再验证密码，防枚举攻击
- 角色体系：单角色字段（USER/ADMIN/CONTENT_OPERATOR），注册默认 USER；角色写入 JWT claim，过滤器读取并授予 Spring Security 权限；`/api/v1/admin/**` 路径需 ADMIN 或 CONTENT_OPERATOR 角色

#### 内容模块（待实现）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `destination` | 目的地信息 | id, name, country, province, description, best_season, cover_url, image_urls(JSON), created_at |
| `attraction` | 景点信息 | id, destination_id(FK→destination), name, description, location(GIS), ticket_price, cover_url, image_urls(JSON), created_at |
| `guide` | 攻略内容 | id, author_id(FK→users), title, content, destination_id, cover_url, image_urls(JSON), status(DRAFT/PUBLISHED/REJECTED), view_count, like_count, created_at, updated_at |

**图片字段说明**：
- `cover_url`：封面图 URL，用于列表卡片展示（单张）
- `image_urls`：JSON 数组 `["url1", "url2", ...]`，存储内容图片，支持多图浏览（景点相册、目的地图集、攻略配图）

#### 行程模块（待实现）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `itinerary` | 行程主表 | id, user_id(FK→users), title, destination_id, cover_url, start_date, end_date, status(DRAFT/PUBLISHED/ARCHIVED), share_code(分享短码), view_count, like_count, created_at, updated_at |
| `itinerary_day` | 行程日程 | id, itinerary_id(FK→itinerary), day_number, title, sort_order |
| `itinerary_item` | 行程明细 | id, itinerary_day_id(FK→itinerary_day), type(HOTEL/ATTRACTION/TRANSPORT/FOOD/ACTIVITY), name, location, time_slot, description, sort_order |

**行程分享**：`share_code` 为 6 位 Base62 短码，生成分享链接时使用，支持取消/过期失效。

#### 盲盒模块（待实现）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `blind_box_template` | 盲盒模板 | id, name, type(DAILY/LIMITED), price, stock, rule_config(JSON), status(ACTIVE/INACTIVE), created_at |
| `blind_box_order` | 盲盒订单 | id, user_id(FK→users), template_id(FK→blind_box_template), order_no, status(PENDING/PAID/OPENED/REFUNDED/CANCELLED), pay_amount, pay_method, pay_time, expire_at, created_at |
| `blind_box_result` | 盲盒结果 | id, order_id(FK→blind_box_order), destination, theme, result_text(LONGTEXT JSON), ~~result_image_url(OSS)~~, share_code(短码), share_expires_at, opened_at |
| `blind_box_preference` | 盲盒预选参数 | id, order_id(FK→blind_box_order), departure_city, budget_level(ECONOMY/STANDARD/LUXURY), theme, image_tags(JSON), created_at |

**盲盒结果说明**：
- `result_text`：完整旅行方案 JSON（目的地/行程/酒店/交通/预算明细），开盒时生成
- `result_image_url`：~~AI 生成的可视化行程单图片~~（**暂不实现**：AI 结果图生成功能尚未就绪）
- `share_code` / `share_expires_at`：结果分享链接短码及过期时间

**预选参数说明**：
- 用户在购买前提交的个性化参数（出发地、预算、主题）
- `image_tags`：~~AI 分析参考图片返回的特征标签~~（**暂不实现**：AI 图片分析接口尚未就绪）

#### 通用模块（待实现）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `collection` | 收藏关系 | id, user_id(FK→users), target_type(guide/destination/itinerary), target_id, created_at |

---

## 八、部署与运维（暂不部署）

### 部署架构

```
用户 → CDN → Nginx（反向代理）→ Spring Boot 应用集群
                                    ↓
                              MySQL（主从）
                                    ↓
                              Redis（集群）
                                    ↓
                              RabbitMQ / Kafka
                                    ↓
                              阿里云 OSS
```

### 环境规划

| 环境 | 用途 | 部署方式 |
|------|------|---------|
| 开发环境 | 日常开发调试 | Docker Compose |
| 测试环境 | 集成测试、QA | Docker Compose |
| 预发布环境 | 上线前验证 | K8s Namespace |
| 生产环境 | 线上服务 | K8s 集群 |

### 监控与运维

- **应用监控**：Spring Boot Actuator + Prometheus + Grafana
- **日志管理**：ELK Stack（Elasticsearch + Logstash + Kibana）
- **告警通知**：企业微信/钉钉机器人
- **数据库备份**：每日全量备份 + Binlog 增量备份

---

## 九、开发排期与里程碑

### Phase 1：MVP 基础版（2–3 周）

| 周次 | 目标 | 交付物 |
|------|------|-------|
| W1 | 前端骨架 + 用户体系 + 数据库设计 | 可登录的前端、用户模块 API |
| W2 | 目的地/攻略内容模块 + 行程规划 | 内容浏览、行程 CRUD |
| W3 | 盲盒基础流程 + 订单支付 | 盲盒购买/开盒全流程 |

### Phase 2：核心功能完善（2–3 周）

| 周次 | 目标 | 交付物 |
|------|------|-------|
| W4 | AI 图片识别 + 智能路线生成 | AI 集成、盲盒方案优化 |
| W5 | 后台管理系统 | 内容/盲盒/订单管理后台 |
| W6 | 运营功能 + 分享 + 通知 | 活动配置、社交分享、消息推送 |

### Phase 3：优化与上线（1–2 周）

| 周次 | 目标 | 交付物 |
|------|------|-------|
| W7 | 性能优化 + 安全加固 | 压测报告、安全扫描通过 |
| W8 | 灰度发布 + 正式上线 | 生产环境部署、监控就绪 |

---

## 十、从设计文档提取的未解决问题

> 以下问题来自 `docs/design.md` 代码审查记录，尚未修复，后续迭代按优先级处理。

### MEDIUM 优先级

| 编号 | 问题 | 影响 | 建议方案 |
|------|------|------|---------|
| M-1 | `RateLimitInterceptor` 的 `ConcurrentHashMap<String, Bucket>` 无淘汰机制，长期运行 key 数量线性增长 | 内存泄漏，高流量下实例重启才能释放 | 替换为 Caffeine `Cache<String, Bucket>`（`expireAfterAccess(10, MINUTES)`，设置 `maximumSize`） |
| M-2 | `StubSmsServiceImpl` 验证码内存缓存未做并发安全的原子操作（put + expiry 检查分两步） | 极低概率的并发竞态，验证码可被重用 | 将存储结构改为 `ConcurrentHashMap<String, CodeEntry>`，用 `compute` 原子替换 |
| M-3 | `UserDetailsServiceImpl.loadUserByUsername` 按 email 查找，而 `AuthServiceImpl.login` 走独立的 `findUserByCredential`，两套查找路径不同步 | 未来若 `UserDetailsService` 路径被某场景激活，行为与预期不符 | 统一 `UserDetailsServiceImpl` 支持 email / phone 查找，或将 `findUserByCredential` 提取为共用私有方法 |

### LOW 优先级

| 编号 | 问题 | 影响 | 建议方案 |
|------|------|------|---------|
| L-1 | `password_reset_tokens` 使用后标记 `used=true` 但不清理，旧记录长期驻留 | DB 表持续增长 | 定时任务每天清理 `used=true` 或 `expires_at < NOW() - 7天` 的记录 |
| L-2 | `refresh_tokens` 中已吊销/过期的 token 不自动清理 | DB 表持续增长 | 定时任务清理 `revoked=true` 或 `expires_at < NOW()` 的记录 |
| L-3 | `ChangePasswordRequest` 无旧密码强度 / 新旧密码相同校验 | 用户可将密码改为与旧密码相同的值而无感知 | 在 `UserServiceImpl.changePassword` 中加相等性检查 |
| L-4 | `UserConverter` 将 `UserStatus` 枚举直接 `.name()` 暴露给前端，枚举重命名会破坏 API 协议 | 接口不稳定 | 定义固定的字符串常量映射，与枚举内部名解耦 |
| L-5 | Refresh Token 轮换后，旧的 Access Token（JWT）仍然有效，直到自然过期（默认 15 分钟） | Access Token 泄露或用户 Logout 后，15 分钟窗口期内仍可访问 API | 见下方方案分析，暂不改 |

---

## 十一、内容模块实现后待办

> 以下内容模块代码已创建（Entity/Mapper/DTO/Converter/Service/Controller 共 42 个 Java 文件），但以下配置尚未完成，需在后续步骤中补充。

### 1. Spring Security 权限配置

- **问题**：管理端接口（`/api/v1/admin/**`）需要 ADMIN 角色权限保护，当前尚未配置
- **需要**：在 Security 配置中添加 `/api/v1/admin/**` 需要 `ROLE_ADMIN` 的规则
- **涉及文件**：`SecurityConfig.java`、`JwtAuthenticationFilter.java`
- **同时**：需确认 JWT 过滤器覆盖 content 模块的所有路径

#### 管理员角色方案 — 已实现（2026-05-19 更新）

用户表已增加 `role` 字段（TINYINT），采用单角色字段方案：

| 角色 | 值 | 权限 |
|------|-----|------|
| USER | 0 | 普通用户：发布攻略、评论、收藏、行程、盲盒 |
| ADMIN | 1 | 管理员：所有 `/api/v1/admin/**` + 内容审核 |
| CONTENT_OPERATOR | 2 | 内容运维：攻略审核、评论管理、目的地/景点 CRUD |

**实现要点：**
- JWT token 中携带 `"role"` claim（如 `"role":"ADMIN"`）
- `JwtAuthenticationFilter` 读取 role 并授予 `ROLE_XXX` 权限
- `SecurityConfig` 配置 `/api/v1/admin/**` 需 ADMIN 或 CONTENT_OPERATOR
- 注册时默认角色为 USER，refreshToken 时从 DB 读取最新 role 写入新 JWT
- 手动执行 `ALTER TABLE users ADD COLUMN role TINYINT NOT NULL DEFAULT 0` 迁移已有数据

==需要对 user 进行重构，需要有多种角色，管理员、用户、内容运维人员，需要进一步分析==





### 2. 文件上传配置 ✅ 已完成

- `application.properties` 已添加 `app.upload.dir`、`app.upload.base-url`、`spring.servlet.multipart.*` 配置

### 3. MyBatis Plus 分页插件 ✅ 已完成（自动配置）

- MyBatis Plus 3.5.12 `spring-boot3-starter` 已通过 `MybatisPlusAutoConfiguration` 自动注册分页插件
- `Page` + `selectPage` 可直接使用，无需手动注册

### 4. 评论删除路径 ✅ 已完成

- `CommentController.delete()` 已改为独立路径 `DELETE /api/v1/comments/{commentId}`

### 5. 静态资源映射 ✅ 已完成

- `WebConfig.addResourceHandlers()` 已注册 `/uploads/**` → `file:uploads/` 映射

### 6. 数据库表创建

- **问题**：`docs/content_tables.sql` 已创建，但尚未执行
- **需要**：在 MySQL 中执行 SQL 文件创建 4 张表（destinations、attractions、guides、comments）

### 7. JWT Access Token 吊销机制讨论（L-5 详细说明）

**问题背景**：

系统采用双令牌设计（Access Token + Refresh Token），职责分离：

- **Access Token（JWT，短期 15 分钟）**：用于每次 API 请求的身份验证。无状态，服务端不查数据库，仅验证签名和过期时间。
- **Refresh Token（长期 30 天）**：仅在 Access Token 过期时用来换取新的 Access Token。有状态，存数据库，支持吊销。

正常刷新流程：客户端调 `/api/v1/auth/refresh` → 吊销旧 Refresh Token → 生成新的 Access Token + 新的 Refresh Token。

**但旧 Access Token 仍然有效**，因为 JWT 是无状态的，`JwtAuthenticationFilter` 只验证签名和过期时间，不查数据库。

**触发 refresh 接口的场景**：
- 前端收到 HTTP 401（Access Token 过期）时自动调用
- 前端预判 Token 快过期时主动调用

**多 Access Token 同时存在的原因**：
- 多设备登录（手机和电脑各有 token）
- 多窗口同时触发 refresh
- 正常场景下无害，但带来安全窗口期

**三种解决方案**：

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| A. Token 版本号 | User 表加 `tokenVersion` 字段，每次刷新 +1，JWT 携带版本号，过滤器比对数据库版本 | 改动小，性能好 | 每次请求多一次数据库查询 |
| B. 缩短 Access Token 过期时间 | 改为 5-10 分钟，旧 token 快速自然过期 | 零改动 | 不是真正的吊销，刷新更频繁 |
| C. Redis 黑名单 | 刷新/登出时将旧 token jti 加入 Redis 黑名单 | 真正的实时吊销 | 引入 Redis 依赖 |

**当前决策**：暂不修改。15 分钟安全窗口对 MVP 阶段可接受，MVP 之后再根据安全需求选择方案。
