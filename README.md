# 趣行旅行平台

面向年轻人的社交化旅游 Web 平台：「内容种草 → 行程规划 → 盲盒探索」的闭环。个人学习与本地演示项目，不对外运营。

P0–P3 主线已完成：用户与 JWT 双 Token、内容社区与审核、多日行程与分享、盲盒（日常 + 限定秒杀）、后台管理与运营配置。当前以本地可复现为准，无 CI。

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Java 25 + Spring Boot 3.5.13、MyBatis Plus、MySQL 8.4、Spring Security + JWT、Bucket4j |
| 中间件 | Redis 8.10（缓存 / Redisson 锁 / Lua 预扣）、RabbitMQ 4.4（异步削峰、死信队列） |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios |
| AI | 阿里云 DashScope 通义千问（生成旅行方案） |
| 构建 | Maven Wrapper；前端 `npm` |

## 快速开始

1. 启动中间件（WSL2）：`wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl start mysql redis-server rabbitmq-server'`
2. 导入建表与示例数据：[`ctrip-backend/docs/SQL/`](ctrip-backend/docs/SQL/)，顺序见 [INSTALL.md](INSTALL.md)
3. 后端：`cd ctrip-backend; .\mvnw.cmd spring-boot:run`（默认 `8080`）
4. 前端：另开终端 `cd ctrip-frontend; npm install; npm run dev`（默认 `3000`，代理到 `8080`）

组件版本、数据库脚本顺序、Redis/RabbitMQ 配置、`403` 排查与验证命令见 [INSTALL.md](INSTALL.md)。

## 目录结构

```
ctrip/
├── API.md                 # 接口契约：约定 + 端点索引（入库）
├── INSTALL.md             # 安装与本地运行（入库）
├── ctrip-backend/         # Spring Boot 后端，src + 测试 + SQL + E2E 脚本
├── ctrip-frontend/        # Vue 3 前端
├── deploy/                # jar 构建与 nginx 启停
└── docs/                  # 本地文档（接口明细、模块设计、阶段结论），不入库
```

## 核心实现要点

- **双 Token 认证**：Access 15 分钟 + Refresh 15 天轮换；Refresh Token 只存 SHA-256 哈希；登录先判封禁再验密码，防用户枚举；认证端点按 IP 限流。
- **秒杀三级演进**：日常购买走 Redisson 锁 + `stock >= 1` 条件更新；限定抢购走 Redis Lua 原子预扣 + RabbitMQ 异步建单；每小时比对 Redis 与 MySQL 库存。
- **三层缓存**：布隆过滤器拦截不存在 ID、空值缓存防击穿、TTL 随机偏移防雪崩。
- **异步开盒**：请求写 Outbox 后经 MQ 消费生成方案，订单 `PAID → PROCESSING → OPENED`，前端轮询取结果。
- **权限三级**：`USER` / `CONTENT_OPERATOR` / `ADMIN`，管理端路径由后端统一校验。

## 文档

| 想做什么 | 去哪里 |
|---|---|
| 跑起来 | [INSTALL.md](INSTALL.md) |
| 查接口 | [API.md](API.md)（约定与端点），字段示例在本地 `docs/api/` |
| 部署演练 | [deploy/DEPLOYMENT.md](deploy/DEPLOYMENT.md) |
| 看变更 | [CHANGELOG.md](CHANGELOG.md) |
| 给 agent 的约定 | [AGENTS.md](AGENTS.md) |
| 架构与模块实现 | 本地 `docs/architecture.md`、`docs/modules/`（不入库） |

## 范围与限制

- 不提供机票/酒店比价与预订；不做公网部署与发版。
- 生成旅行方案依赖 DashScope，无网时开盒失败属预期，不是 bug。
- 压测与秒杀失败回归只能在隔离 QA 环境（独立库、vhost、Redis DB）执行。
- 没有 CI：`git push` 不触发检查，验证只能本地跑。

## 验证

| 范围 | 命令 |
|---|---|
| 后端测试 | `cd ctrip-backend; .\mvnw.cmd test` |
| 前端规则 / 构建 | `cd ctrip-frontend; npm run test`、`npm run build` |
| API 级 E2E | `python ctrip-backend/scripts/run_all_e2e.py`（需后端在 8080） |
