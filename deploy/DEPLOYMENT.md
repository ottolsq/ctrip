# 部署试运行（Windows jar + WSL nginx）

> 仅用于本地部署演练/答辩演示，使用 `application.properties` 中的开发凭据，禁止直接用于公网。

## 布局

| 组件 | 位置 | 端口 |
|------|------|------|
| Spring Boot jar | Windows，`ctrip-backend/target/backend-*.jar` | 8081 |
| nginx（用户态，无需 sudo） | WSL `~/.local/ctrip-nginx` | 8088 |
| 前端静态文件 | WSL `~/.local/state/ctrip-deploy/html`（来自 `ctrip-frontend/dist`） | 由 nginx 托管 |
| MySQL / Redis / RabbitMQ | WSL 现有服务 | 3306 / 6379 / 5672 |

nginx 将 `/api/`、`/uploads/` 反代到 `127.0.0.1:8081`，其余路径走 SPA `index.html`。

## 前置条件

- Windows 已安装 JDK 25，默认路径 `/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot`（可用 `JAVA_HOME` 覆盖）。
- Node/npm、Maven Wrapper 可用。
- WSL Ubuntu 能 `apt-get download`（首次会下载 nginx 1.24 包，无需 sudo）。
- 8081、8088 未被占用。

## 构建

```bash
cd c:/Users/ottol/myproject/ctrip
bash deploy/build.sh
```

产物：`ctrip-backend/target/backend-*.jar`、`ctrip-frontend/dist/`。

## 启动

```bash
bash deploy/start.sh
```

成功后访问：

- 前端：<http://localhost:8088/>
- 接口：<http://localhost:8088/api/v1/banners>
- 后端直连：<http://localhost:8081/>

## 验证

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/api/v1/banners
BASE_URL=http://localhost:8088/api/v1 python -I ctrip-backend/scripts/e2e_admin_p14.py
```

## 停止

```bash
bash deploy/stop.sh
```

## 日志与排查

- 后端：`deploy/run/backend.log`
- nginx：WSL `~/.local/state/ctrip-deploy/logs/{access,error}.log`
- 8081 被占用：`netstat -ano | grep 8081`，或先执行 `deploy/stop.sh`。
- nginx 未启动：在 WSL 执行 `bash deploy/nginx/setup-nginx.sh` 检查下载；确认 `~/.local/ctrip-nginx/usr/sbin/nginx -v` 可用。
- WSL 访问 Windows 8081 失败：确认 `.wslconfig` 使用 `networkingMode=mirrored`，并在 WSL 执行 `curl http://localhost:8081/api/v1/banners`。
- 前端深链接 404：检查 nginx 配置中的 `try_files $uri $uri/ /index.html`。
