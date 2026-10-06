# MTC (Modern Testing Capabilities) 智能测试平台

AI 驱动的自动化测试平台。输入需求描述，AI 自动生成测试用例、测试脚本和测试数据。

## ✨ 功能特性

- 🧪 **AI 测试用例生成** — 基于需求描述自动生成完整的测试用例（正常/异常/边界场景）
- 📜 **AI 测试脚本生成** — 自动生成 Playwright + TypeScript 自动化测试脚本
- 📊 **AI 测试数据生成** — 自动生成配套的测试数据（JSON 格式）
- ▶️ **在线测试执行** — 生成脚本后一键执行，实时查看执行日志和结果
- 📈 **执行结果统计** — 展示通过率、失败用例、执行耗时等详细统计
- ⚡ **SSE 流式输出** — 生成和执行过程实时展示，不用等
- 📁 **项目管理** — 多项目管理，测试资产分类存放
- 🔐 **JWT 登录认证** — 安全的用户身份验证
- 🎯 **基础 URL 配置** — 生成的脚本自动带入待测应用地址
- 💾 **脚本保存管理** — 脚本可保存到项目，便于后续复用和执行

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + JWT |
| AI 服务 | Python 3 + FastAPI + LangGraph + DeepSeek LLM |
| 数据库 | PostgreSQL 16 |
| 缓存 | Redis 7 |
| 迁移工具 | Flyway |
| API 文档 | springdoc / FastAPI（Swagger UI） |
| 通信 | HTTP/REST + SSE（流式） |

## 📂 项目结构

```
.
├── web/              # 前端 (Vue 3)
├── backend/          # 后端 (Spring Boot)
├── ai-service/       # AI 服务 (FastAPI + LangGraph)
├── demo-app/         # 演示用待测应用（TodoList）
├── demo-tests/       # 演示用 Playwright 测试示例
├── docker-compose.yml
├── .env.example
└── docs/             # 技术文档
```

## 🚀 快速开始

### 前置依赖

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| Docker & Docker Compose | 最新版 | 运行 PostgreSQL 和 Redis |
| JDK | 21+ | 后端运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端运行 |
| Python | 3.10+ | AI 服务运行 |
| DeepSeek API Key | - | AI 生成能力 |

### 启动步骤

#### 1. 克隆项目

```bash
git clone git@github.com:ScenicCloud/mtc-platform.git
cd mtc-platform
```

#### 2. 启动数据库和缓存

```bash
docker compose up -d postgres redis
```

等待容器健康检查通过（约 10 秒）。

#### 3. 配置 AI 服务 API Key

```bash
cd ai-service
cp .env.example .env
```

编辑 `.env`，填入你的 DeepSeek API Key：

```
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxx
```

安装依赖并启动：

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

验证：访问 http://localhost:8000/health 返回 `{"status":"ok"}`

#### 4. 启动后端

```bash
cd backend
MTC_DEMO_ENABLED=true mvn spring-boot:run
```

验证：
- 健康检查：http://localhost:8080/actuator/health
- API 文档：http://localhost:8080/swagger-ui.html

#### 5. 启动前端

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

### Docker Compose 一键启动（可选）

如果你不想本地装 Java / Node / Python，可以用 Docker 全栈启动：

```bash
# 先配置 API Key
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY

# 全栈启动
docker compose up -d --build
```

访问：http://localhost:5173

## 📖 使用指南

### 基本流程

```
填写需求描述 + 基础 URL
        │
        ▼
  生成测试用例（AI）
        │
        ├─→ 保存到项目
        │
        ├─→ 生成测试脚本（Playwright）
        │       │
        │       ├─→ 保存到项目
        │       └─→ ▶ 一键执行测试
        │                │
        │                ▼
        │         实时日志 + 结果统计
        │
        └─→ 生成测试数据（JSON）
```

### 操作步骤

1. **登录平台** — 打开 http://localhost:5173，使用 admin/admin123 登录
2. **创建项目** — 点击"新建项目"，填写项目名称
3. **进入测试设计** — 点击项目进入测试设计页面
4. **填写需求** — 在左侧输入需求描述和待测应用的基础 URL
5. **生成测试用例** — 点击"生成测试用例"，AI 自动生成
6. **保存到项目** — 点击"保存到项目"，用例存入数据库
7. **生成脚本/数据** — 切换到对应标签，选择用例后生成
8. **保存脚本** — 在测试脚本标签点击"保存到项目"
9. **一键执行** — 点击"执行测试"，实时查看执行日志和结果统计
10. **下载使用** — 复制或下载生成的脚本和数据

### Demo 应用

项目附带一个 TodoList 待办事项应用，专门用来练手测试。

**方式一：Docker 启动（推荐）**

```bash
docker compose up -d demo-app
```

访问：http://localhost:3000

**方式二：本地启动**

```bash
cd demo-app
python3 -m http.server 3000
```

- 演示账号：`demo` / `demo123`
- 功能：登录、注册、待办列表（增删改查、筛选、清除已完成）

> 💡 **在线执行提示**：如果使用 Docker Compose 启动所有服务，在测试设计页面的"基础 URL"中填写 `http://demo-app`（Docker 内部网络地址），即可直接在平台上执行测试脚本访问 Demo 应用。

### 运行 Playwright 测试

```bash
cd demo-tests
npm install
npx playwright test
```

示例结果：8 个登录测试用例全部通过。

## ⚠️ 已知限制

- **AI 生成脚本格式**：部分情况下 AI 生成的 Playwright 脚本可能存在格式问题（缺少空格/换行），导致无法直接执行。遇到此问题时，可以：
  1. 将脚本复制到本地 IDE 中，使用 Prettier 格式化后再执行
  2. 手动调整脚本格式
  3. 使用 `demo-tests/` 目录下的示例脚本进行测试
- **被测应用选择器**：AI 生成的脚本使用通用语义化选择器，实际项目中需要根据真实页面结构调整选择器
- **浏览器支持**：V1.0 默认使用 Chromium 浏览器执行测试

## 🏗️ 架构说明

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

关键变量：

| 变量 | 说明 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥（AI 服务必需） |
| `POSTGRES_DB` | PostgreSQL 数据库名 |
| `POSTGRES_USER` | PostgreSQL 用户名 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 |
| `MTC_JWT_SECRET` | JWT 签名密钥 |
| `MTC_DEMO_ENABLED` | 是否启用演示账号 |

## 📚 文档

- API 文档（后端）：http://localhost:8080/swagger-ui.html
- API 文档（AI 服务）：http://localhost:8000/docs
- 技术选型文档：`docs/选型决策表.md`
- 接口契约：`docs/接口契约.md`
- 验收标准：`docs/验收标准.md`

## 🤝 开发协作

### 分支策略

- `main` — 主分支，始终可运行
- 功能开发请从 `main` 切出分支，开发完成后提 PR

### 提交规范

使用语义化提交：

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
refactor: 重构
chore: 构建/工具链调整
```

## 📄 License

MIT
