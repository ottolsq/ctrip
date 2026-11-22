# 本地开发环境（WSL2）

> 更新日期：2026-11-20
> 适用环境：Windows 11 + WSL2 Ubuntu 24.04，仅用于本地开发与答辩演示。

## 1. 组件版本

| 组件 | 版本 | 安装来源 |
|------|------|----------|
| JDK | Eclipse Temurin 25.0.5 LTS | winget |
| MySQL | 8.4.13 LTS | Oracle MySQL APT 仓库（`mysql-8.4-lts`） |
| Redis | 8.10.3 | packages.redis.io APT 仓库 |
| RabbitMQ | 4.4.1 | GitHub 通用二进制包 |
| Erlang/OTP | 27.3.4.18 | Launchpad `ppa:rabbitmq/rabbitmq-erlang` |
| Node.js | 26.x | 用户环境已安装 |
| Maven | Maven Wrapper（`./mvnw`） | 项目自带 |

选择 WSL2 原生服务而不是 Docker：当前网络无法访问 Docker Hub；WSL2 + systemd 已可用，安装与排障更直接。

## 2. 启动与停止

```bash
# 启动 WSL 与三个中间件（systemd 自启）
wsl -d Ubuntu-24.04 -u root -- bash -lc \
  'systemctl start mysql redis-server rabbitmq-server'

# 查看状态
wsl -d Ubuntu-24.04 -u root -- bash -lc \
  'systemctl is-active mysql redis-server rabbitmq-server'

# 保持 WSL 常驻（避免空闲自动停止导致 Windows 侧端口不可达）
wsl -d Ubuntu-24.04 -u root -- sleep infinity

# 停止
wsl -d Ubuntu-24.04 -u root -- bash -lc \
  'systemctl stop rabbitmq-server redis-server mysql'
```

Windows 侧通过 WSL localhost forwarding 访问 `127.0.0.1` 的 3306 / 6379 / 5672 / 15672。

## 3. MySQL 8.4

### 3.1 账号与数据库

- 地址：`localhost:3306`
- 账号：`root / 123456`
- 数据库：`ctrip`，字符集 `utf8mb4`

`application.properties` 的连接串已包含：

```text
useSSL=false&allowPublicKeyRetrieval=true
```

原因：MySQL 8 默认使用 `caching_sha2_password`，非 SSL 连接需要允许公钥检索（仅开发环境）。

### 3.2 初始化数据

按依赖顺序导入 DDL，再导入示例数据：

```bash
cd /mnt/c/Users/ottol/myproject/ctrip/ctrip-backend/docs/SQL
for f in ctrip_user.sql ctrip_content.sql ctrip_itinerary.sql ctrip_blindbox.sql \
         ctrip_admin.sql ctrip_p3_compliance.sql ctrip_blindbox_import.sql \
         ctrip_user_demo_data.sql ctrip_content_demo_data.sql \
         ctrip_itinerary_demo_data.sql ctrip_blindbox_demo_data.sql \
         ctrip_admin_seed.sql; do
  mysql -uroot -p123456 ctrip < "$f"
done
```

- `ctrip_admin.sql` 是 P1 迁移脚本，**仅执行一次**；重复执行会因列已存在而失败。
- `ctrip_p3_compliance.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可重复执行。
- `ctrip_blindbox_import.sql` 为一次性 ALTER，重复执行会因列已存在而失败。
- 导入后共 18 张表；示例数据包含 5 个用户、5 个目的地、6 篇攻略、8 条行程、5 个盲盒模板。

### 3.3 后台种子账号（仅开发）

| 账号 | 密码 | 角色 |
|------|------|------|
| admin@example.com | Admin12345 | ADMIN |
| operator@example.com | Operator12345 | CONTENT_OPERATOR |

由 `ctrip_admin_seed.sql` 写入，仅用于本地开发与答辩演示。

## 4. Redis 8.10

- 地址：`localhost:6379`
- 无密码（开发环境）
- 监听：`0.0.0.0`；`protected-mode no`（仅开发环境，便于 Windows 侧访问）

## 5. RabbitMQ 4.4.1

- AMQP：`localhost:5672`，账号 `guest / guest`
- 管理台：http://localhost:15672（`guest / guest`）
- 安装目录：`/opt/rabbitmq_server-4.4.1`（软链 `/opt/rabbitmq`）
- 配置：`/etc/rabbitmq/rabbitmq.conf`
- 数据：`/var/lib/rabbitmq`
- 日志：`/var/log/rabbitmq`
- systemd 服务：`rabbitmq-server.service`

`rabbitmqctl` 需以服务用户执行，避免 Erlang cookie 不一致：

```bash
su -s /bin/sh rabbitmq -c \
  'HOME=/var/lib/rabbitmq RABBITMQ_HOME=/opt/rabbitmq /opt/rabbitmq/sbin/rabbitmqctl status'
```

关键配置：

```text
listeners.tcp.1 = 0.0.0.0:5672
loopback_users.guest = false
management.tcp.ip = 0.0.0.0
management.tcp.port = 15672
```

## 6. 启动应用

```bash
# 后端（8080）
cd ctrip-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run

# 前端（3000，代理到 8080）
cd ctrip-frontend
npm install
npm run dev
```

Vite 监听 `0.0.0.0:3000`，因此 `http://localhost:3000` 与 `http://127.0.0.1:3000` 均可访问。该配置仅用于开发，不要在生产环境暴露开发服务器。

## 7. 验证

### 7.1 后端测试

```bash
cd ctrip-backend
./mvnw test
```

当前基线：**181/181 通过**（P3 + 导入 + 秒杀入口测试）。`CtripApplicationTests` 使用 H2，并通过
`app.cache.bloom.enabled=false` 关闭启动时的布隆过滤器预加载；定时任务的首次执行
延迟 1 分钟，避免测试上下文在空库上触发查询。

### 7.2 API 级端到端

后端启动后执行：

```bash
python -I ctrip-backend/scripts/e2e_p0.py
```

覆盖路径：

1. 邮箱注册与登录；
2. 攻略收藏 / 收藏状态 / 我的收藏 / 取消收藏；
3. 行程创建 → 发布 → 分享（免登录读取，`isOwner=false`）→ 归档 → 重新发布；
4. 行程收藏与取消；
5. 盲盒下单 → 模拟支付 → 异步开盒（RabbitMQ + AI 失败回退）→ 结果查询。

脚本会创建新的测试用户与订单，可重复执行。

### 7.3 管理端 P1.1 E2E

需先执行 `ctrip_admin_seed.sql`，并重启后端加载 P1.1 代码：

```bash
python -I ctrip-backend/scripts/e2e_admin_p11.py
```

覆盖：

1. 管理员登录与用户列表；
2. 注册普通用户 → 关键词检索 → 用户详情；
3. 封禁 → 旧 access token 立即 401；
4. 解封 → 恢复原状态并重新登录；
5. 内容运营访问 `/admin/users`、`/admin/blind-box/templates` → 403。

### 7.4 内容运营 P1.2 E2E

需先重启后端加载 P1.2 代码：

```bash
python -I ctrip-backend/scripts/e2e_admin_p12.py
```

覆盖：作者创建/提交攻略 → 运营驳回（理由）→ 作者修改重提 → 运营通过 → 公共可见；编辑已发布转审核中；评论检索/删除；内容数据；普通用户访问管理端 403。

### 7.5 盲盒后台 P1.3 E2E

需先重启后端加载 P1.3 代码，并确保 RabbitMQ 正常（退款库存恢复经 MQ 异步完成）：

```bash
python -I ctrip-backend/scripts/e2e_admin_p13.py
```

覆盖：DAILY 库存强制 -1 且编辑不报错；LIMITED 活动窗口/库存校验；非法模板 400；窗口内下单库存 5→4；活动结束/未开始均 400；仅改名不覆盖并发库存；管理端订单列表/详情（日期为 ISO 字符串）；开盒后按 OPENED 退款，库存恢复 5 且仅一次；重复退款、删除有订单模板 400；内容运营/普通用户访问管理端 403。

### 7.6 运营与数据概览 P1.4 E2E

需先重启后端加载 P1.4 代码：

```bash
python -I ctrip-backend/scripts/e2e_admin_p14.py
```

覆盖：Banner 创建/启用窗口/停用/删除与公开过滤；推荐位目的地/攻略装配，未发布攻略跳过；Dashboard 字段与 7 天序列；运营访问管理端 403；公开接口免登录。

### 7.7 全量 E2E、压测与部署试运行（P2）

一键运行全部 E2E（429 自动等待重试）：

```bash
python -I ctrip-backend/scripts/run_all_e2e.py
```

并发压测（默认库存 10、并发 20）：

```bash
python -I ctrip-backend/scripts/stress_blindbox.py
```

部署试运行：Windows jar 8081 + WSL 用户态 nginx 8088（无需 sudo）：

```bash
bash deploy/build.sh
bash deploy/start.sh
BASE_URL=http://127.0.0.1:8088/api/v1 python -I ctrip-backend/scripts/e2e_admin_p14.py
bash deploy/stop.sh
```

完整结果见 `docs/other/p2_acceptance.md`，操作细节见 `deploy/DEPLOYMENT.md`。

### 7.8 合规能力 P3 E2E

先执行迁移（可重复执行）：

```bash
wsl -d Ubuntu-24.04 -- bash -lc "mysql -uroot -p123456 ctrip < /mnt/c/Users/ottol/myproject/ctrip/ctrip-backend/docs/SQL/ctrip_p3_compliance.sql"
```

重启后端加载 P3 代码后执行：

```bash
python -I ctrip-backend/scripts/e2e_admin_p3.py
```

覆盖：概率公示 70/25/5 与合计 100、保底 10、开盒后用户抽取记录（档位/目的地/版本）、管理员按用户检索、普通用户访问管理员记录 403。

> 注意：同一 RabbitMQ 队列只能有一个后端实例消费。执行 E2E 时不要同时运行 8080 与 8081 两个实例，否则开盒事件可能被旧实例消费。

P3 起结果生成队列升级为 `blindbox.result.gen.v2.queue`（带死信参数、消费失败重试 3 次后转 `dlx.exchange`）。旧队列 `blindbox.result.gen.queue` 不再被消费，确认无积压后可在 RabbitMQ 管理台删除。

盲盒导入行程需执行一次迁移：

```bash
wsl -d Ubuntu-24.04 -- bash -lc "mysql -uroot -p123456 ctrip < /mnt/c/Users/ottol/myproject/ctrip/ctrip-backend/docs/SQL/ctrip_blindbox_import.sql"
```

`e2e_p0.py` 已覆盖开盒 → 导入行程 → 重复导入幂等 → 行程天/明细校验。

### 7.9 限时抢购 E2E

```bash
python -I ctrip-backend/scripts/e2e_flash_sale.py
```

覆盖：限定模板创建 → Lua 抢购 → MQ 异步 PAID 订单 → 重复购买拒绝 → 售罄 → 开盒（验证偏好已保存）。

## 8. 安全说明

以上配置仅用于本地开发：

- MySQL 使用开发密码；
- Redis 无密码且关闭保护模式；
- RabbitMQ 允许 guest 远程登录；
- JWT secret 与 AI API Key 为开发占位值。

生产环境必须改为环境变量注入、强密码、最小权限账号与受限网络访问。
