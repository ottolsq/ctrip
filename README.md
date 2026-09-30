# ctrip 旅行盲盒网站

前后端合并仓库，整合原独立维护的后端与前端代码。

## 项目结构

```
ctrip/
├── ctrip-backend/    # Java 后端项目（Spring Boot）
│   ├── src/main/java/com/ctrip/...
│   ├── docs/         # 接口文档与设计文档
│   └── pom.xml
├── ctrip-frontend/   # 前端项目（Vite + 原生 JS）
│   ├── src/          # 页面源码
│   ├── index.html
│   └── package.json
└── README.md
```

## 历史来源

- `ctrip-backend`：原仓库 [ottolsq/ctrip](https://github.com/ottolsq/ctrip)
- `ctrip-frontend`：原仓库 [ottolsq/ctrip-frontend](https://github.com/ottolsq/ctrip-frontend)

两个仓库通过 `git subtree` 合并，保留各自完整提交历史。
