# AGENTS.md — ctrip

## 命令

```powershell
# 中间件（从 Windows 启动 WSL2 服务）
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl start mysql redis-server rabbitmq-server'
```

```powershell
# 后端（从仓库根目录执行）
Set-Location .\ctrip-backend
.\mvnw.cmd spring-boot:run  # 默认 8080
# 测试时在后端目录单独执行
.\mvnw.cmd test
```

```powershell
# 前端（另开终端，从仓库根目录执行）
Set-Location .\ctrip-frontend
npm install
npm run dev                 # 默认 3000，已代理到 8080
npm run test                # node --test，业务规则
npm run build
```

```powershell
# API 级 E2E（从仓库根目录执行，需开发后端已启动）
python ctrip-backend/scripts/run_all_e2e.py
```

在 WSL 中，从后端目录执行 `bash ./mvnw spring-boot:run` 或 `bash ./mvnw test`。启动排障见[安装与本地运行](INSTALL.md)。

## 禁止事项

- 不要提交 `docs/`：根 `.gitignore` 已忽略，任务书与计划只留本地
- 不要把真实密钥或生产口令写进 `application.properties`，也不要提交到仓库
- 文档（含示例、截图、历史记录和本地文档）不得写入任何具体密码、密钥、Token、验证码、个人联系方式或含用户名的本机路径，本地默认值也不例外。保留字段名，使用明显占位符、环境变量名或交互输入；提交前复查。
- 不要在没有跑过对应 `e2e_*.py` 的情况下改下单、开盒、秒杀链路
- 不要绕过 `SecurityConfig` 的鉴权；盲盒支付回调这类白名单要显式保留在配置里
- 不要新增依赖而不在 `pom.xml` / `package.json` 里写明用途

## 环境事实

- Windows 11 + WSL2 Ubuntu 24.04；JDK 25（Temurin）、Node 26、MySQL 8.4、Redis 8.10、RabbitMQ 4.4
- MySQL 库 `ctrip`；RabbitMQ 控制台 `localhost:15672`；连接凭据使用本地开发配置。
- AI 需要 DashScope key；无网时盲盒生成失败，属预期而非 bug
- 没有 CI：`git push` 不触发任何检查，验证只能本地跑

## 完成定义

- 改后端接口：`.\mvnw.cmd test`（WSL：`bash ./mvnw test`）通过 + 对应 `e2e_*.py` 通过
- 改前端业务规则：`npm run test` 通过；改页面：`npm run build` 通过
- 改部署脚本：同步更新 `deploy/DEPLOYMENT.md`

## 文档指针

- 目标与范围：`docs/goal.md`（本地，不提交）
- 架构与模块设计：`docs/architecture.md`、`docs/modules/`（本地，不提交）
- 接口契约：[API.md](API.md)；接口明细 `docs/api/`（本地，不提交）
- 安装与本地运行：[INSTALL.md](INSTALL.md)
- 技术债：`docs/plan.md` 的技术债表（本地，不提交）
- 阶段结论：`docs/history/`（本地，不提交）
