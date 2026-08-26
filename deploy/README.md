# 部署说明

本项目支持三种部署方式，按需选择。

## 方式一：Docker Compose 一键编排（推荐）

前置条件：安装 Docker + Docker Compose。

```bash
# 进入 deploy 目录
cd deploy

# 可选：接入真实大模型
# 在环境变量中设置 LLM_PROVIDER=dashscope LLM_API_KEY=xxx LLM_MODEL=qwen-plus
# 或直接编辑 docker-compose.yml

# 构建并启动全部服务
docker compose up -d --build

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f ai-service
```

服务地址：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost |
| 后端 API | http://localhost:1234 |
| Swagger 文档 | http://localhost:1234/swagger-ui.html |
| AI 服务 | http://localhost:8001/docs |

说明：
- MySQL 首次启动自动导入 `数据库/db_aps.sql`。
- AI 服务默认 `LLM_PROVIDER=mock`（离线演示）；接入真实模型后智能导购/文案/分析返回真实内容。
- 数据卷 `mysql-data` / `redis-data` / `faiss-data` 持久化，容器重建不丢数据。

## 方式二：Kubernetes 部署

```bash
# 1. 先构建镜像并推送到镜像仓库（示例按本地镜像处理）
docker build -t ai-shopping-ai:2.0.0 ../ai-service
docker build -t ai-shopping-backend:2.0.0 ../springboot
docker build -t ai-shopping-frontend:2.0.0 ../vue

# 2. 部署
kubectl apply -f deploy/k8s/
kubectl get pods
```

> 生产环境需额外部署 MySQL / Redis 并调整 `deploy/k8s/*.yaml` 中的连接配置。

## 方式三：本地开发模式

分别启动三个服务（详见根目录 README「快速开始」）：

1. MySQL（导入 `数据库/db_aps.sql`）
2. AI 微服务：`uvicorn app.main:app --port 8001`（需先 `python scripts/init_vector_store.py`）
3. SpringBoot 后端：`mvn spring-boot:run`（配置 `application.properties`）
4. Vue 前端：`npm run serve`
