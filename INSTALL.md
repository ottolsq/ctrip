# 安装与本地运行

> 适用环境：Windows 11 + WSL2 Ubuntu 24.04，本地开发与演示。CI 不覆盖本文步骤，命令都是手工执行。

## 1. 组件版本

| 组件 | 版本 | 安装来源 |
|---|---|---|
| JDK | Eclipse Temurin 25.0.5 LTS | winget |
| MySQL | 8.4.13 LTS | Oracle MySQL APT 仓库（`mysql-8.4-lts`） |
| Redis | 8.10.3 | packages.redis.io APT 仓库 |
| RabbitMQ | 4.4.1 + Erlang/OTP 27.3.4.18 | GitHub 通用二进制包 / `ppa:rabbitmq/rabbitmq-erlang` |
| Node.js | 26.x | 本机环境 |
| Maven | Maven Wrapper（`.\mvnw.cmd` / `bash ./mvnw`） | 项目自带，无需另装 |

选 WSL2 原生服务而不是 Docker：当前网络无法访问 Docker Hub，WSL2 + systemd 已可用。

## 2. 启动与停止中间件

```bash
# 启动（systemd 自启）
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl start mysql redis-server rabbitmq-server'

# 查看状态
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl is-active mysql redis-server rabbitmq-server'

# 保持 WSL 常驻，避免空闲自动停止后 Windows 侧端口不可达
wsl -d Ubuntu-24.04 -u root -- sleep infinity

# 停止
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl stop rabbitmq-server redis-server mysql'
```

Windows 侧通过 WSL localhost forwarding 访问 `127.0.0.1` 的 3306 / 6379 / 5672 / 15672。

## 3. 数据库

```bash
# 在 WSL 中、仓库根目录执行；先设置本地 MySQL 账号
export MYSQL_USER=<本地账号>          # 口令由 -p 交互输入
cd ctrip-backend/docs/SQL
for f in ctrip_user.sql ctrip_content.sql ctrip_itinerary.sql ctrip_blindbox.sql \
         ctrip_admin.sql ctrip_p3_compliance.sql ctrip_blindbox_import.sql \
         ctrip_optimistic_lock.sql ctrip_flash_sale_unique.sql \
         ctrip_user_demo_data.sql ctrip_content_demo_data.sql \
         ctrip_itinerary_demo_data.sql ctrip_blindbox_demo_data.sql \
         ctrip_admin_seed.sql; do
  mysql -u "$MYSQL_USER" -p ctrip < "$f" || exit 1
done
```

- 库名 `ctrip`，字符集 `utf8mb4`，导入后共 18 张表。
- `ctrip_admin.sql`、`ctrip_blindbox_import.sql`、`ctrip_optimistic_lock.sql`、`ctrip_flash_sale_unique.sql` 是一次性脚本，重复执行会报列/索引已存在。
- `ctrip_p3_compliance.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可重复执行。
- 示例数据含 5 个用户、5 个目的地、6 篇攻略、8 条行程、5 个盲盒模板；`ctrip_admin_seed.sql` 提供 `ADMIN` 与 `CONTENT_OPERATOR` 种子账号，账号口令自行在本地设定，不要写进任何文档或命令。
- 连接串需要 `useSSL=false&allowPublicKeyRetrieval=true`：MySQL 8 默认 `caching_sha2_password`，非 SSL 连接需允许公钥检索（仅开发环境）。

## 4. Redis 与 RabbitMQ

- Redis：`localhost:6379`，开发环境无密码，`protected-mode no`，便于 Windows 侧访问。
- RabbitMQ：AMQP `localhost:5672`，控制台 `http://localhost:15672`，账号用本机配置。
  - 安装目录 `/opt/rabbitmq_server-4.4.1`（软链 `/opt/rabbitmq`），数据 `/var/lib/rabbitmq`，日志 `/var/log/rabbitmq`。
  - `rabbitmqctl` 需以服务用户执行，否则 Erlang cookie 不一致：
    ```bash
    su -s /bin/sh rabbitmq -c 'HOME=/var/lib/rabbitmq RABBITMQ_HOME=/opt/rabbitmq /opt/rabbitmq/sbin/rabbitmqctl status'
    ```
  - 关键配置：`listeners.tcp.1 = 0.0.0.0:5672`、`loopback_users.guest = false`、`management.tcp.port = 15672`。

## 5. 启动应用

```powershell
# 后端（仓库根目录，默认 8080）
Set-Location .\ctrip-backend
.\mvnw.cmd -v
.\mvnw.cmd spring-boot:run
```

```powershell
# 前端（另开终端，仓库根目录）
Set-Location .\ctrip-frontend
npm install
npm run dev
```

WSL 内把 `.\mvnw.cmd` 换成 `bash ./mvnw`。开发入口 `http://localhost:3000`（端口以 Vite 输出为准），`/api` 与 `/uploads` 转发到 `8080`。

- 页面能开但列表加载失败：先确认开发后端在 `8080`，只有部署演练后端 `8081` 不够。
- 不要同时运行共用同一套 RabbitMQ 队列的两个后端实例，也不要把 Vite 暴露到公网。

## 6. Maven Wrapper 报 403

`.\mvnw.cmd -v` 在 `DownloadFile` 报 `403` 说明 Maven 发行包下载失败，Spring Boot 还没启动。版本与地址见 `.mvn/wrapper/maven-wrapper.properties`。

1. 地址恢复后重跑 `.\mvnw.cmd -v`，看到 Maven 与 Java 版本再启动应用。
2. 本机已有同版本 Maven 时，可临时用 `mvn -v` 或全路径 `mvn.cmd` 代替——这只是绕过，不代表 Wrapper 已恢复。
3. 不要把这台机器的缓存路径写进仓库配置。

## 7. 验证

| 范围 | 命令 |
|---|---|
| 后端单元/集成测试 | `cd ctrip-backend; .\mvnw.cmd test`（WSL：`bash ./mvnw test`） |
| 前端业务规则 | `cd ctrip-frontend; npm run test` |
| 前端构建 | `cd ctrip-frontend; npm run build` |
| API 级 E2E | `python ctrip-backend/scripts/run_all_e2e.py`（需开发后端在 8080） |
| 单阶段 E2E | `python ctrip-backend/scripts/e2e_p0.py`、`e2e_admin_p11.py` … `e2e_admin_p3.py` |
| 秒杀并发 | `python ctrip-backend/scripts/stress_blindbox.py`（仅隔离 QA 后端与独立库/vhost/Redis DB） |

E2E 会累积测试用户与订单，跑完用 `cleanup_e2e.py` 清理。压测与秒杀失败回归必须在隔离环境跑，不要指向演示库。

## 8. 安全

- `application.properties` 只放结构，数据库口令、RabbitMQ 账号、JWT 签名密钥、DashScope key 通过环境变量或本机未入库配置注入。
- 文档（含示例、截图、历史记录）不写任何具体口令、密钥、Token、验证码、个人联系方式或含用户名的本机路径；保留字段名，用占位符或环境变量名。
- Redis 无密码、`protected-mode no` 仅限本机开发；任何对外暴露前必须改掉。
