# AI 智能商品销售系统

[![CI](https://github.com/hO-personGit/AI-Shopping-System/actions/workflows/ci.yml/badge.svg)](https://github.com/hO-personGit/AI-Shopping-System/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/)
[![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.6-4FC08D)](https://vuejs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688)](https://fastapi.tiangolo.com/)
[![LangChain](https://img.shields.io/badge/LangChain-0.3-1C3C3C)](https://www.langchain.com/)

一个在传统电商系统基础上扩展 **AI 应用能力** 的全栈项目，覆盖 Vue 前端、SpringBoot 后端、MySQL 数据库与独立 FastAPI AI 微服务，实现了**智能导购、AI 文案生成、AI 销售分析**三大 AI 能力，并具备完整的**工程化（CI/CD、Docker 容器化、Git 协作）**体系。

## ✨ 核心功能

**传统电商业务**

- 用户端：商品浏览、分类搜索、购物车、收藏、下单支付、评价、物流跟踪
- 后台：商品、分类、订单、库存（出入库）、公告、轮播图、用户、角色权限管理
- ECharts 销售统计可视化

**AI 能力**

- 🤖 **AI 智能导购**：基于用户自然语言需求，结合商品向量检索 + 大模型生成推荐结果与购买建议
- ✍️ **AI 商品文案生成**：管理员录入商品基础信息，自动生成标题、简介、详情、营销语
- 📊 **AI 销售分析**：基于订单、销量、库存数据，输出热销分析、库存预警、补货建议
- 💬 **多轮对话**：智能导购支持会话上下文，追问更自然
- ⚡ **SSE 流式输出**：导购回答打字机式逐字返回，体验更流畅
- 🔍 **混合检索 + 语义精排**：向量检索 + 关键词多路召回 + RRF 融合 + Rerank 精排，提升推荐精度
- 🛠 **Function Calling Agent**：AI 可调用商品查询、库存查询等工具，能力可扩展
- 🧪 **RAG 评估闭环**：recall@k / MRR / NDCG@k + 忠实度 / 相关性评估报告（RAGAS 风格）

**高并发与工程化（v3.0）**

- 🚀 **MQ 异步下单链路**：RabbitMQ 削峰解耦，Redis 预扣库存防超卖，延迟队列超时关单 + 定时兜底
- 🔄 **订单状态机**：0 待支付 → 1 已支付 → 2 已发货 → 3 已完成 / 4 已取消 / 5 退款中 → 6 已退款 / 7 退款失败
- 🛡 **缓存三防**：布隆过滤器防穿透、互斥锁双检防击穿、TTL 抖动防雪崩（Caffeine + Redis 两级缓存）
- 📈 **性能压测量化**：JMeter / Python 压测脚本（QPS / P50 / P95 / P99 / 超卖检查）

## 🏗 系统架构

```mermaid
flowchart LR
    subgraph Frontend [Vue 前端 :8080]
        U[用户端页面]
        A[后台管理端]
        AG[AiGuideAssistant 多轮对话浮窗]
    end

    subgraph Backend [SpringBoot 后端 :1234]
        C[Controller 层]
        S[Service 层]
        M[MyBatis-Plus / Mapper]
        AI[AiService 调用 AI 微服务]
        RC[Redis 缓存 · 分布式锁]
    end

    subgraph AIService [FastAPI AI 微服务 :8001]
        RA[RAG 检索链路]
        VS[FAISS 向量库]
        AGENT[Function Calling Agent]
        LLM[大模型 API 兼容层<br/>mock / qwen / openai / zhipu]
        CACHE[问答结果缓存]
    end

    subgraph Data [数据层]
        DB[(MySQL db_aps)]
        RDS[(Redis)]
    end

    U --> C
    A --> C
    AG --> AI
    C --> S
    S --> M --> DB
    S --> RC --> RDS
    AI --> RA
    RA --> VS
    RA --> AGENT
    AGENT --> LLM
    AGENT --> CACHE
    RA --> CACHE
end
```

**架构要点**

- 前端**不直接**调用 FastAPI，SpringBoot 作为业务网关统一收敛 `/ai/**` 请求
- SpringBoot **不直接**调用大模型 API，AI 能力全部收敛在 Python 微服务，便于替换模型 / 扩展能力
- 三层解耦：后续更换向量库、升级模型或新增 AI 功能，只需改动 AI 微服务

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 2 · Element UI · Axios · ECharts · Vue Router · Vuex |
| 后端 | Spring Boot 3.4 · MyBatis-Plus 3.5 · Spring Security · JWT · Redis · Caffeine · RabbitMQ |
| AI 服务 | Python · FastAPI · LangChain · FAISS · Pydantic |
| 数据库 | MySQL 8 · Redis · RabbitMQ |
| 工程化 | Git/GitHub · GitHub Actions CI · Docker · docker-compose |

## 📁 目录结构

```text
AI-Shopping-System/
├── .github/            # GitHub Actions CI + Issue/PR 模板
├── .vscode/            # VSCode 调试/扩展配置
├── springboot/         # SpringBoot 后端
├── vue/                # Vue 前端
├── ai-service/         # FastAPI AI 微服务
│   ├── app/            #  应用代码（main / schemas / services）
│   ├── tests/          #  单元测试（pytest）
│   └── scripts/        #  向量库初始化脚本
├── docker-compose.yml  # 一键编排 MySQL+Redis+AI+后端+前端
├── CONTRIBUTING.md     # Git 协作开发规范
└── README.md
```

## 🚀 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
docker compose up -d
```

> 启用 MQ 异步下单（可选）：
> ```bash
> # 1) 执行数据库迁移（新增 order_no 幂等键）
> mysql -uroot -p db_aps < 数据库/upgrade/v3.0.0_mq_async_order.sql
> # 2) 开启 MQ 开关后重启后端
> APP_MQ_ENABLED=true docker compose up -d --build backend
> # RabbitMQ 管理台 http://localhost:15672 (guest/guest)
> ```

> 需先准备 `springboot/src/main/resources/application.properties` 与 `ai-service/.env`（见方式二）。

### 方式二：本地启动

**1. MySQL**：导入 `数据库/db_aps.sql`，确认 `db_aps` 库可连接。

**2. AI 微服务**

```bash
cd ai-service
python -m venv .venv
.\.venv\Scripts\activate          # Windows
pip install -r requirements.txt
copy .env.example .env            # 默认 LLM_PROVIDER=mock 可离线演示
python scripts/init_vector_store.py
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
# 健康检查 http://localhost:8001/health
```

**3. SpringBoot 后端**（端口 1234）

```bash
cd springboot
copy src\main\resources\application-example.properties src\main\resources\application.properties
# 修改数据库密码等参数后启动
mvn spring-boot:run
```

**4. Vue 前端**（端口 8080）

```bash
cd vue
npm install
npm run serve
```

访问：前端 `http://localhost:8080` · 后端接口 `http://localhost:1234` · AI 服务 `http://localhost:8001/docs`

## 🤝 Git 协作开发

本项目采用 `main + develop + feature/*` 分支模型，详见 [CONTRIBUTING.md](./CONTRIBUTING.md)：

```bash
git checkout develop && git pull origin develop
git checkout -b feature/my-feature
# ... 开发并测试 ...
git commit -m "feat(ai): 描述本次改动"
git push origin feature/my-feature   # 发起 PR → develop
```

## 🧪 质量保障

- 后端：`mvn test`（JUnit5 + MockMvc）
- 前端：`npm run lint && npm run build`
- AI 服务：`pytest`
- CI：GitHub Actions 自动执行以上全部检查

## 📄 更多文档

- [AI 功能与部署说明](文档/AI功能部署说明.md)
- [Docker 部署说明](deploy/README.md)

## 📌 版本

- v3.0.0：MQ 异步下单 + 订单状态机 + 缓存三防 + Rerank 精排 + RAG 评估闭环 + 性能压测量化
- v2.0.0：AI 能力升级（多轮对话、SSE 流式、混合检索、Function Calling、问答缓存）+ 工程化（CI、Docker、Git 协作规范）
- v1.0.0：AI 基础能力（智能导购、文案生成、销售分析）
