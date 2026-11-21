# P2 测试、并发压测与部署试运行验收记录

> 执行日期：2026-11-21
> 环境：Windows 11 + WSL2 Ubuntu 24.04；JDK 25.0.5；Node 26.13；MySQL 8.4 / Redis 8.10 / RabbitMQ 4.4。
> 部署：Windows jar（8081）+ WSL 用户态 nginx 1.24.0（8088）。

## 1. 自动化结果

| 项目 | 命令 | 结果 |
|------|------|------|
| 后端全量测试 | `cd ctrip-backend && ./mvnw test` | **155/155 通过** |
| 前端规则单测 | `cd ctrip-frontend && npm test` | **4/4 通过** |
| 前端构建 | `cd ctrip-frontend && npm run build` | 通过 |
| 全部 API E2E（经 nginx） | `BASE_URL=http://127.0.0.1:8088/api/v1 python -I ctrip-backend/scripts/run_all_e2e.py` | **ALL E2E PASS（144.3s）** |
| 并发压测（经 nginx） | `BASE_URL=http://127.0.0.1:8088/api/v1 python -I ctrip-backend/scripts/stress_blindbox.py` | **STRESS PASS** |
| 部署首页 | `curl -w "%{http_code}" http://localhost:8088/` | 200 |
| 部署 API | `curl -w "%{http_code}" http://localhost:8088/api/v1/banners` | 200 |
| 部署 E2E | `BASE_URL=http://127.0.0.1:8088/api/v1 python -I .../e2e_admin_p14.py` | `E2E ADMIN P1.4 PASS` |

E2E 运行器实测发生 1 次注册 429：等待 65 秒后重跑 `e2e_admin_p12.py` 成功，其余脚本一次通过。

## 2. 并发压测结果

```text
模板: LIMITED stock=10
requests=20 success=10 failed=10
wall=0.261s rps=76.7 p95=241.7ms stock_after=0
失败全部为 400 库存不足
```

结论：20 并发下成功订单恰为 10，库存归零，无 5xx、无超卖。

## 3. 部署试运行

| 组件 | 位置 | 端口 | 证据 |
|------|------|------|------|
| Spring Boot jar | Windows `ctrip-backend/target/backend-0.0.1-SNAPSHOT.jar` | 8081 | 启动日志 `Started CtripApplication` |
| nginx | WSL `~/.local/ctrip-nginx`（用户态，无 sudo） | 8088 | `nginx/1.24.0 (Ubuntu)` |
| 前端 dist | WSL `~/.local/state/ctrip-deploy/html` | 8088 | 首页 200，含 `<div id="app">` |
| 反向代理 | `/api/`、`/uploads/` → 127.0.0.1:8081 | — | 公开接口 200，E2E PASS |

操作步骤见 `deploy/DEPLOYMENT.md`。

> Python/urllib 访问 `localhost:8088` 时可能因 IPv6 回退等待约 20 秒，脚本统一使用 `127.0.0.1`。

## 4. 功能检查清单

| 检查项 | 验证方式 | 结果 |
|--------|----------|------|
| 内容运营点击“后台管理”进入攻略审核而非 403 | `router/admin.js` 角色感知 redirect + 构建 | 代码路径通过，**建议浏览器人工点验** |
| 已封禁用户不显示“封禁”按钮 | `businessRules.test.js` `canBanUser` | 通过 |
| 盲盒确认订单页显示规则 description | `businessRules.test.js` `ruleDescription` | 通过 |
| 模板编辑保留未知 `ruleConfig` 字段 | `businessRules.test.js` `mergeRuleConfig` | 通过 |
| 首页轮播/推荐有数据展示、无数据隐藏 | 公开 API E2E + `v-if` + 空 slot 断言 | 接口通过，**视觉待人工确认** |
| 数据概览数字加载 | Dashboard API E2E + 前端构建 | 接口通过，**视觉待人工确认** |
| 普通用户/运营访问管理端 403 | P1.4 E2E（6 次 403） | 通过 |
| 非法枚举/请求体返回 400 | `GlobalExceptionHandlerTest` | 通过 |
| 审核并发后到者 400 | `AdminGuideServiceImplTest` 条件更新 0 行 | 通过 |

## 5. 本阶段修复的 Minor

1. 全局非法查询参数/请求体 → 400。
2. 攻略审核改为 `WHERE status=PENDING` 条件更新。
3. 用户管理封禁按钮排除 `SUSPENDED`。
4. 后台模板列表 `limit` 限制 1..100。
5. 删除模板外键冲突转 400。
6. `OrderCreate` 正确解析字符串 `ruleConfig`。
7. 模板编辑保留未知规则字段。
8. 补 P1.2 非 PENDING 驳回、审核条件更新 0 行测试。

## 6. 仍遗留（不在 P2 范围）

- 公开推荐位逐条查询（N+1）；
- Dashboard 全表加载用户/攻略/订单；
- 管理端编辑无乐观锁；
- E2E 数据无自动清理；
- 浏览器级 UI 自动化测试未引入（人工清单 + 纯函数单测替代）。
