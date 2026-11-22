# 趣行旅行平台

面向年轻人的社交化旅游 Web 平台，采用"内容种草 → 智能规划 → 盲盒探索"的业务闭环。

---

## 核心功能

| 模块 | 说明 |
|------|------|
| 用户认证 | 手机号/邮箱注册登录、JWT 双 Token 认证、个人资料、密码管理 |
| 内容社区 | 目的地、景点、攻略发布/浏览、评论、点赞、收藏 |
| 行程规划 | 自由创建多日行程，景点/交通/住宿关联、拖拽排序、分享 |
| 旅游盲盒 | 日常盲盒 + 限定秒杀盲盒，AI 生成旅行方案；P3 概率公示/保底/抽取记录 |
| 后台管理 | P1.4 已完成：后台布局/角色守卫、用户管理、攻略审核、评论管理、内容数据、盲盒模板与订单管理（含退款/异常重试）、轮播图、推荐位、平台数据概览 |

---

## 技术栈

### 后端 `ctrip-backend/`

| 层级 | 技术选型 |
|------|---------|
| 语言/框架 | Java 25 + Spring Boot 3.5.13 |
| 数据访问 | MyBatis Plus + MySQL 8.4 LTS |
| 认证安全 | Spring Security + JWT (HS256) + BCrypt |
| 缓存/锁 | Redis 8.10 + Redisson 3.37（布隆过滤器、Lua 脚本、分布式锁） |
| 消息队列 | RabbitMQ 4.4（异步削峰、事件驱动、死信队列） |
| AI 服务 | 阿里云 DashScope 通义千问 |
| 构建/测试 | Maven + JUnit 5 + Mockito + AssertJ |

### 前端 `ctrip-frontend/`

| 层级 | 技术选型 |
|------|---------|
| 框架 | Vue 3 + Vite |
| UI 组件 | Element Plus |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| HTTP | Axios |
| 富文本 | WangEditor |

---

## 项目结构

```
ctrip/
├── ctrip-backend/          # 后端 Spring Boot 项目
│   ├── src/main/java/com/ctrip/   # 业务代码
│   ├── src/test/java/com/ctrip/ # 单元/集成测试
│   ├── docs/                # 接口文档、设计文档、SQL
│   ├── uploads/             # 本地图片上传目录
│   └── pom.xml
├── ctrip-frontend/         # 前端 Vue 3 项目
│   ├── src/
│   │   ├── views/           # 页面视图
│   │   ├── components/      # 公共组件
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # Pinia 状态
│   │   └── api/             # 接口封装
│   ├── index.html
│   └── package.json
├── docs/                    # 任务书、设计与实施计划（本地文档，不纳入版本库）
└── README.md
```

---

## 后端核心亮点

1. **企业级 JWT 双 Token 认证**
   - Access Token 15 分钟 + Refresh Token 15 天轮换
   - Refresh Token 仅存储 SHA-256 哈希，原始值不落库
   - 账号不存在与密码错误返回统一信息，防用户枚举
   - Bucket4j 令牌桶按 IP 限流，防暴力破解
   - 三级角色权限：USER / ADMIN / CONTENT_OPERATOR

2. **高并发秒杀三级演进**
   - 日常购买：Redisson 分布式锁 + MySQL 乐观锁
   - 限定抢购：Redis Lua 脚本原子预扣 + RabbitMQ 异步削峰
   - 设计文档保留完整秒杀链路（网关限流 + Lua + MQ + 对账）
   - `@Scheduled` 每小时 Redis↔MySQL 库存对账

3. **三层缓存防穿透**
   - 布隆过滤器拦截不存在 ID
   - Redis 空值缓存防反复击穿
   - 参数校验兜底
   - TTL 随机偏移防雪崩，Cache-Aside 保证最终一致

4. **RabbitMQ 异步事件驱动**
   - 4 个 Exchange + 10 个 Queue 覆盖订单、开盒、用户行为、内容审核
   - 同步开盒改造为异步：支付后立即返回，MQ 消费调用 AI 生成方案
   - 手动 ACK + 指数退避重试 + 死信队列 + Redis SETNX 幂等

---

## 快速开始

### 本地开发环境（WSL2）

当前开发环境为 Windows 11 + WSL2 Ubuntu 24.04：

| 组件 | 版本 |
|------|------|
| JDK | Eclipse Temurin 25.0.5 |
| MySQL | 8.4.13 LTS |
| Redis | 8.10.3 |
| RabbitMQ | 4.4.1（Erlang/OTP 27.3） |
| Node.js | 26.x |

启动中间件：

```bash
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl start mysql redis-server rabbitmq-server'
```

- MySQL：本地数据库 `ctrip`；连接账号使用本机开发配置。
- RabbitMQ 管理台：`http://localhost:15672`；用本地配置的账号登录。
- 详细安装、配置、数据库导入与 API 级 E2E 说明见 [ctrip-backend/docs/other/dev_environment.md](ctrip-backend/docs/other/dev_environment.md)

### 后端

启动前确认 MySQL、Redis、RabbitMQ 已运行，并按[本地开发指南](ctrip-backend/docs/other/dev_environment.md)初始化数据库、核对 `application.properties`。

```powershell
# Windows PowerShell：从仓库根目录执行
Set-Location .\ctrip-backend
.\mvnw.cmd spring-boot:run
```

```bash
# WSL：从仓库根目录执行，使用 WSL 中的 JDK
cd ctrip-backend
bash ./mvnw spring-boot:run
```

如果 Maven Wrapper 下载报 `403`，看[Wrapper 排查](ctrip-backend/docs/other/dev_environment.md#maven-wrapper-下载报-403)。

### 前端

在另一个终端从仓库根目录执行：

```powershell
cd ctrip-frontend
npm install
npm run dev
```

开发时访问 `http://localhost:3000`（实际端口以 Vite 输出为准），Vite 将 `/api` 和 `/uploads` 转发到后端 `8080`。部署演练则使用 nginx `8088` 和后端 jar `8081`。

---

## 文档

根目录 `docs/` 存放毕业设计任务书、设计与实施计划（本地文档，不纳入版本库）。

| 想做什么 | 从哪里读 |
|---|---|
| 启动与联调 | [本地开发指南](ctrip-backend/docs/other/dev_environment.md)、[数据库脚本](ctrip-backend/docs/SQL/) |
| 查看接口 | [用户](ctrip-backend/docs/user_api.md)、[内容](ctrip-backend/docs/content_api.md)、[行程](ctrip-backend/docs/itinerary_api.md)、[盲盒](ctrip-backend/docs/blindbox_api.md)、[管理端](ctrip-backend/docs/admin_api.md) |
| 部署演练 | [Windows jar + WSL nginx](deploy/DEPLOYMENT.md) |
| 查验收或遗留问题 | [P2 阶段验收](ctrip-backend/docs/other/p2_acceptance.md)、[技术债登记](ctrip-backend/docs/other/technical_debt.md) |
| 回看设计 | [模块与业务流程设计](ctrip-backend/docs/ctrip_modules.md)、[盲盒模块设计](ctrip-backend/docs/blindbox_module.md) |
| 图片上传 | [接口用法](ctrip-backend/docs/content_api.md#上传接口)、[存储与校验](ctrip-backend/docs/content_module.md) |

### 开发阶段与当前范围

早期 MVP 从用户与认证开始，逐步加入目的地与攻略、多日行程与分享、盲盒购买与开盒。后台管理和运营功能随后加入。前端现已覆盖用户端和管理端。

- P0 用户端补齐了攻略收藏和行程分享。参见[内容 API](ctrip-backend/docs/content_api.md)、[行程 API](ctrip-backend/docs/itinerary_api.md)和 [P0 回归脚本](ctrip-backend/scripts/e2e_p0.py)。
- P1.1 在现有前端加入 `/admin`，提供用户列表、封禁与解封功能。前端按角色显示菜单，后端校验操作权限。参见[管理端 API](ctrip-backend/docs/admin_api.md)和 [P1.1 回归脚本](ctrip-backend/scripts/e2e_admin_p11.py)。
- P3 公开上架盲盒的各档概率和保底次数，并提供用户抽取记录及管理员查询。参见[盲盒 API](ctrip-backend/docs/blindbox_api.md#概率公示)、[管理端 API](ctrip-backend/docs/admin_api.md)和 [P3 回归脚本](ctrip-backend/scripts/e2e_admin_p3.py)。
- 盲盒开盒后，订单所有者可把方案导入为草稿行程。重复导入返回同一行程。参见[导入接口](ctrip-backend/docs/blindbox_api.md#开盒结果导入行程)和 [P0 回归脚本](ctrip-backend/scripts/e2e_p0.py)。

项目用于个人学习和本地演示，不提供机票或酒店比价与预订。演示可从浏览攻略和创建行程开始，再完成盲盒模拟支付、异步开盒和结果导入行程。生成旅行方案需要 DashScope。接口和运行方式见上表。

攻略和目的地列表支持关键词筛选，不提供全站统一搜索。行程和盲盒结果的分享链接有效期均为 7 天；行程取消分享后，原链接立即失效。

### 验证脚本

后端 Maven 测试可在 PowerShell 中运行。进入 `ctrip-backend` 后执行 `.\mvnw.cmd test`。对应源码见[缓存读写测试](ctrip-backend/src/test/java/com/ctrip/content/cache/CacheServiceTest.java)、[订单状态机测试](ctrip-backend/src/test/java/com/ctrip/blindbox/service/BlindBoxOrderStateMachineTest.java)、[秒杀消费者测试](ctrip-backend/src/test/java/com/ctrip/blindbox/messaging/FlashSaleOrderConsumerTest.java)和[开盒消息投递测试](ctrip-backend/src/test/java/com/ctrip/blindbox/messaging/OpenRequestOutboxTest.java)。

以下脚本位于 [`ctrip-backend/scripts/`](ctrip-backend/scripts/)：

- `scripts/e2e_p0.py`：P0 用户端 API 级端到端验证脚本
- `scripts/e2e_admin_p11.py`：P1.1 管理端 API 级端到端验证脚本
- `scripts/e2e_admin_p12.py`：P1.2 内容运营 API 级端到端验证脚本
- `scripts/e2e_admin_p13.py`：P1.3 盲盒后台/库存/退款 API 级端到端验证脚本
- `scripts/e2e_admin_p14.py`：P1.4 轮播图/推荐位/数据概览 API 级端到端验证脚本
- `scripts/run_all_e2e.py`：一键运行全部 API E2E（含 429 重试）；每次子脚本运行限时 300 秒，超时终止并报告已有输出
- `scripts/stress_blindbox.py`：LIMITED 库存并发压测脚本
- `scripts/e2e_admin_p3.py`：P3 概率/保底/抽取记录 API 级端到端验证脚本
- `scripts/e2e_flash_sale.py`：限定盲盒秒杀 API 端到端验证，覆盖顺序抢购和未预热模板的并发首抢
- 秒杀失败回归：仅在本地隔离 QA 后端（8080、独立 MySQL 库、RabbitMQ vhost 和 Redis DB）运行。安装有 `redis-cli` 的环境可用 `FLASH_E2E_REDIS_DB=13 python3 ctrip-backend/scripts/e2e_flash_sale.py`；DB 号必须与 QA 后端一致。脚本会对新建测试模板注入一份陈旧的 Redis 库存。

---
