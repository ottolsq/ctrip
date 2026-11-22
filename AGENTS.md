# AGENTS.md — ctrip

## 命令

```powershell
# 中间件（WSL2）
wsl -d Ubuntu-24.04 -u root -- bash -lc 'systemctl start mysql redis-server rabbitmq-server'

# 后端
cd ctrip-backend
./mvnw spring-boot:run      # 默认 8080
./mvnw test                 # 34 个测试类

# 前端
cd ctrip-frontend
npm install
npm run dev                 # 默认 3000，已代理到 8080
npm run test                # node --test，业务规则
npm run build

# API 级 E2E（需后端已启动）
python ctrip-backend/scripts/run_all_e2e.py
```

## 禁止事项

- 不要提交 `docs/`：根 `.gitignore` 已忽略，任务书与计划只留本地
- 不要把真实密钥或生产口令写进 `application.properties`，也不要提交到仓库
- 不要在没有跑过对应 `e2e_*.py` 的情况下改下单、开盒、秒杀链路
- 不要绕过 `SecurityConfig` 的鉴权；盲盒支付回调这类白名单要显式保留在配置里
- 不要新增依赖而不在 `pom.xml` / `package.json` 里写明用途

## 环境事实

- Windows 11 + WSL2 Ubuntu 24.04；JDK 25（Temurin）、Node 26、MySQL 8.4、Redis 8.10、RabbitMQ 4.4
- MySQL `root/123456`，库 `ctrip`；RabbitMQ 控制台 `localhost:15672`（`guest/guest`）——细节见 README
- AI 需要 DashScope key；无网时盲盒生成失败，属预期而非 bug
- 没有 CI：`git push` 不触发任何检查，验证只能本地跑

## 完成定义

- 改后端接口：`./mvnw test` 通过 + 对应 `e2e_*.py` 通过
- 改前端业务规则：`npm run test` 通过；改页面：`npm run build` 通过
- 改部署脚本：同步更新 `deploy/DEPLOYMENT.md`

## 文档指针

- 目标与范围：`docs/goal.md`（本地，不提交）
- 架构：`docs/architecture.md`（本地，不提交）
- 模块与接口：`ctrip-backend/docs/`
- 技术债：`ctrip-backend/docs/other/technical_debt.md`
