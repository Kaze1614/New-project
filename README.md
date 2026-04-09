# 人工智能高中数学网站（全栈 MVP）

本项目实现了 `Vue3 + Spring Boot + MySQL` 的学生端 MVP，覆盖：
- 登录/注册
- 首页学习工作台（KPI 卡片 + 信息看板）
- 错题本（新增、删除、AI Mock 分析）
- 收藏本
- 待复习任务与完成闭环
- 章节目录树
- 智能搜索
- 多轮智能答疑（AI Mock）

默认内置管理员账号（启动时自动创建）：
- 账号：`admin`
- 密码：`123456`

## 目录结构

- `frontend`：Vue3 前端
- `backend`：Spring Boot 后端
- `deploy`：Docker Compose 部署配置

## 一键启动（推荐）

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 进入部署目录并启动：

```bash
cd deploy
docker compose --env-file ../.env up -d --build
```

3. 访问地址（默认端口）：
- 前端：`http://localhost:18000`
- 后端：`http://localhost:18080`
- MySQL：`localhost:3307`

> 如果你本机没有端口冲突，可在 `.env` 中改回 `80/8080/3306`。

## 本地开发启动

### 后端

建议使用 JDK 17 + Maven：

```bash
cd backend
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认开发代理会把 `/api` 转发到 `http://localhost:18080`。如果你本地后端不是这个端口，可设置环境变量 `VITE_API_PROXY_TARGET`（例如 `http://localhost:8080`）。

## API 概览

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/dashboard/overview`
- `GET/POST/DELETE /api/mistakes`
- `POST /api/mistakes/{id}/analyze`
- `GET/POST/DELETE /api/favorites`
- `GET /api/review/tasks`
- `POST /api/review/tasks/{id}/complete`
- `GET /api/chapters/tree`
- `GET /api/search?keyword=...`
- `POST /api/qa/sessions`
- `GET /api/qa/sessions`
- `GET /api/qa/sessions/{id}/messages`
- `POST /api/qa/sessions/{id}/messages`

## 测试

### 后端

```bash
cd backend
mvn test
```

### 前端

```bash
cd frontend
npm run test
```

## 说明

- 当前 AI 能力为 Mock（`backend/src/main/java/com/datong/mathai/ai/MockAIProvider.java`）。
- 已预留 `AIProvider` 接口，后续可替换真实大模型 API。
- 图片错题目前记录 URL/路径元数据，未启用 OCR。
- 注册接口当前只需要 `username` 与 `password`，`displayName` 会默认使用账号名。
