# 技术架构

## 一句话

Vue 3 单页前端 + Spring Boot 单体后端（26 个 Controller）+ MySQL / Redis / RabbitMQ；AI 方案生成走阿里云 DashScope；部署形态是 WSL 内的 jar + nginx。

## 组件

| 组件 | 职责 | 边界（不负责什么） | 代码位置 |
|---|---|---|---|
| 前端 | 页面、路由、Pinia 状态、接口封装 | 不直连数据库、不持有业务状态 | `ctrip-frontend/src` |
| 后端 | REST 接口、JWT 鉴权、业务编排、MQ 生产与消费 | 不渲染页面 | `ctrip-backend/src/main/java` |
| 后台管理 | 用户 / 内容审核 / 盲盒 / 轮播 / 推荐 / 数据概览 | 复用同一后端，仅靠角色守卫区分 | 同上 |
| 部署脚本 | jar 构建、nginx 启停 | 不负责环境安装 | `deploy/` |
| AI 集成 | DashScope 生成旅行方案 | 不做结果缓存 | 后端 ai 模块 |

## 数据流

```
下单/开盒：前端 → 盲盒接口 → 支付回调 → MQ 事件 → AI 生成方案 → 订单落库
秒杀：前端 → Redis Lua 原子预扣 → 排队 MQ → 订单创建 → 每小时 Redis↔MySQL 对账
```

## 外部依赖

| 依赖 | 用途 | 不可用时的降级 |
|---|---|---|
| MySQL 8.4 | 业务数据 | 启动失败，无降级 |
| Redis 8.10 | 缓存、分布式锁、Lua 预扣 | 秒杀不可用，其余降级为直查 |
| RabbitMQ 4.4 | 异步削峰、事件驱动 | 开盒停在「生成中」，靠重试 |
| 阿里云 DashScope | 生成旅行方案 | 开盒失败可重试，无离线兜底 |

## 已知边界

- 单机部署：nginx 与 jar 都在 WSL 内；`stop.sh` 按端口杀进程，不校验进程身份（OPS-5）
- 没有 CI：测试与 E2E 全靠人工执行脚本，`git push` 不触发任何检查
- `uploads/` 落本地磁盘，未接对象存储
- E2E 会累积测试用户与订单，未自动归档（FLASH-4）
- 秒杀并发只有单测，缺真实并发 E2E（FLASH-1）

## 演进方向

先补 CI 与测试数据隔离，再考虑对象存储与多实例部署；完整债务清单见 `ctrip-backend/docs/other/technical_debt.md`。
