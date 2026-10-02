# MTC (Modern Testing Capabilities) 平台

AI 驱动的自动化测试平台骨架。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + Spring Security + JWT |
| AI 服务 | Python 3 + FastAPI + LangGraph + uvicorn |
| 数据库 | PostgreSQL 16 |
| 缓存 | Redis 7 |
| 迁移工具 | Flyway |
| API 文档 | springdoc / FastAPI（Swagger UI） |
| 通信 | HTTP/REST + SSE（流式） |

## 项目结构

```
.
├── web/              # 前端 (Vue 3)
├── backend/          # 后端 (Spring Boot)
├── ai-service/       # AI 服务 (FastAPI + LangGraph)
├── docker-compose.yml
├── .env.example
└── docs/             # 技术文档
```

## 快速开始

### 前置依赖

- Docker & Docker Compose（运行 PostgreSQL 和 Redis）
- JDK 21+
- Maven 3.9+
- Node.js 18+
- Python 3.10+

### 启动步骤

#### 1. 启动数据库和缓存

```bash
docker compose up -d postgres redis
```

等待容器健康检查通过（约 10 秒）。

#### 2. 启动 AI 服务

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

验证：访问 http://localhost:8000/health 返回 `{"status":"ok"}`

#### 3. 启动后端

```bash
cd backend
MTC_DEMO_ENABLED=true mvn spring-boot:run
```

验证：
- 健康检查：http://localhost:8080/actuator/health
- API 文档：http://localhost:8080/swagger-ui.html

#### 4. 启动前端

```bash
cd web
npm install
npm run dev
```

访问：http://localhost:5173

### 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

> 演示账号在后端启动时自动创建（需要设置 `MTC_DEMO_ENABLED=true`）。

## Docker Compose 一键启动（可选）

如果你不想本地装 Java / Node / Python，可以用 Docker 全栈启动：

```bash
docker compose up -d --build
```

访问：http://localhost:5173

## 开发说明

### 单向依赖

```
web ──HTTP/SSE──▶ backend(Spring) ──HTTP──▶ ai-service(FastAPI)
```

浏览器永远不直接调用 AI 服务，都走后端转发。

### 统一返回格式

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "xxxxxxxx"
}
```

- `code = 0` 表示成功，非 0 表示失败
- `traceId` 用于全链路追踪

### 数据库迁移

使用 Flyway，迁移脚本放在 `backend/src/main/resources/db/migration/`。

新增表：
1. 在 `db/migration/` 下新建 `V2__xxx.sql`
2. 在 `entity/` 下新建实体类
3. 在 `mapper/` 下新建 Mapper 接口

### 环境变量

参考 `.env.example`，复制为 `.env` 后修改。

## 文档

- API 文档（后端）：http://localhost:8080/swagger-ui.html
- API 文档（AI 服务）：http://localhost:8000/docs
- 技术选型文档：`docs/选型决策表.md`
- 接口契约：`docs/接口契约.md`
- 验收标准：`docs/验收标准.md`
