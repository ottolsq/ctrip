# 趣行旅行平台（前后端合并仓库）

面向年轻人的社交化旅游 Web 平台，采用"内容种草 → 智能规划 → 盲盒探索"的业务闭环。

- `ctrip-backend/`：Java 后端服务（原 `ottolsq/ctrip`）
- `ctrip-frontend/`：Vue 3 前端应用（原 `ottolsq/ctrip-frontend`）

两个仓库通过 `git subtree` 合并，保留各自完整提交历史。

---

## 核心功能

| 模块 | 说明 |
|------|------|
| 用户认证 | 手机号/邮箱注册登录、JWT 双 Token 认证、个人资料、密码管理 |
| 内容社区 | 目的地、景点、攻略发布/浏览、评论、点赞、收藏 |
| 行程规划 | 自由创建多日行程，景点/交通/住宿关联、拖拽排序、分享 |
| 旅游盲盒 | 日常盲盒 + 限定秒杀盲盒，AI 生成旅行方案，开盒后一键导入行程 |
| 后台管理 | 用户/内容/盲盒模板/订单管理（角色权限控制） |

---

## 技术栈

### 后端 `ctrip-backend/`

| 层级 | 技术选型 |
|------|---------|
| 语言/框架 | Java 25 + Spring Boot 3.5.13 |
| 数据访问 | MyBatis Plus + MySQL 8.1 |
| 认证安全 | Spring Security + JWT (HS256) + BCrypt |
| 缓存/锁 | Redis + Redisson（布隆过滤器、Lua 脚本、分布式锁） |
| 消息队列 | RabbitMQ（异步削峰、事件驱动、死信队列） |
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

### 后端

```bash
cd ctrip-backend
# 启动前确保 MySQL、Redis、RabbitMQ 已运行，并修改 application.properties
./mvnw spring-boot:run
# 后端默认端口 8080
```

### 前端

```bash
cd ctrip-frontend
npm install
npm run dev
# 前端默认端口 3000，代理已配置到 http://localhost:8080
```

---

## 文档

后端设计文档位于 `ctrip-backend/docs/`：

- `ctrip_modules.md`：功能模块、业务流程、数据库设计
- `ctrip_plan.md`：开发计划、接口设计、MVP 范围
- `SQL/`：MySQL 建表与示例数据脚本
- `other/`：详细方案设计、测试方案、项目亮点

---

## 历史来源

- 后端原仓库：[github.com/ottolsq/ctrip](https://github.com/ottolsq/ctrip)
- 前端原仓库：[github.com/ottolsq/ctrip-frontend](https://github.com/ottolsq/ctrip-frontend)
