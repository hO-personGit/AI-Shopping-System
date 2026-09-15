# AI 智能商品销售系统

一个在传统电商系统上扩展 AI 能力的全栈项目。系统由 Vue 前端、SpringBoot 后端、MySQL 数据库与独立 FastAPI AI 微服务组成。系统提供智能导购、AI 文案生成、AI 销售分析三大能力，并具备完整的工程化体系（CI/CD、Docker、Git 协作）。

## 核心功能

**电商业务**

- 用户端：商品浏览、分类搜索、购物车、收藏、下单支付、评价、物流跟踪
- 后台：商品、分类、订单、库存、公告、轮播图、用户与角色权限管理
- ECharts 销售统计可视化

**AI 能力**

- AI 智能导购：基于自然语言需求，结合商品向量检索与大模型生成推荐
- AI 商品文案生成：自动生成标题、简介、详情与营销语
- AI 销售分析：输出热销分析、库存预警与补货建议
- 多轮对话 + SSE 流式输出：导购支持会话上下文，回答打字机式返回
- 混合检索 + 语义精排：向量与关键词多路召回，RRF 融合 + Rerank 精排
- Function Calling Agent：AI 可调用商品查询、库存查询等工具
- RAG 评估闭环：recall@k / MRR / NDCG@k 评估报告（RAGAS 风格）

**高并发与生产级能力**

- MQ 异步下单：RabbitMQ 削峰解耦，Redis 预扣库存防超卖，延迟队列超时关单
- 订单状态机：待支付 → 已支付 → 已发货 → 已完成 / 已取消 / 退款中 → 已退款
- 缓存三防：布隆过滤器防穿透、互斥锁双检防击穿、TTL 抖动防雪崩
- 限流熔断降级：令牌桶限流、滑动窗口熔断、AI 服务降级兜底
- 可靠消息：雪花 ID、接口幂等、本地消息表最终一致性
- 可观测性：TraceId 全链路透传、Micrometer 指标、Prometheus + Grafana 监控
- 性能压测：QPS / P50 / P95 / P99 实测数据（见 [perf/README.md](perf/README.md)）

## 系统架构

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
        RL[限流 · 熔断]
        MSG[本地消息表]
    end

    subgraph AIService [FastAPI AI 微服务 :8001]
        RA[RAG 检索链路]
        VS[FAISS / Milvus 向量库]
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

- 前端不直接调用 FastAPI，SpringBoot 作为网关统一收敛 `/ai/**` 请求
- SpringBoot 不直接调用大模型 API，AI 能力全部收敛在 Python 微服务
- 三层解耦：更换向量库、升级模型或新增 AI 功能，只需改动 AI 微服务

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 2 · Element UI · Axios · ECharts · Vue Router · Vuex |
| 后端 | Spring Boot 3.4 · MyBatis-Plus · Spring Security · JWT · Redis · Caffeine · RabbitMQ |
| AI 服务 | Python · FastAPI · LangChain · FAISS / Milvus · Pydantic |
| 数据库 | MySQL · Redis · RabbitMQ |
| 工程化 | Git/GitHub · GitHub Actions CI · Docker · docker-compose |

## 目录结构

```text
AI-Shopping-System/
├── .github/            # GitHub Actions CI + Issue/PR 模板
├── .vscode/            # VSCode 调试配置
├── springboot/         # SpringBoot 后端
├── vue/                # Vue 前端
├── ai-service/         # FastAPI AI 微服务
├── deploy/             # Docker / K8s / 监控部署
├── docs/               # 架构设计说明
├── perf/               # 压测脚本与实测数据
├── CONTRIBUTING.md     # Git 协作开发规范
└── README.md
```

## 快速开始

### 方式一：Docker Compose 一键启动

```bash
cd deploy
docker compose up -d
```

### 方式二：本地启动

**1. MySQL**：导入 `数据库/db_aps.sql`（脚本位于仓库同级 `数据库/` 目录），确认 `db_aps` 库可连接。

**2. AI 微服务**（端口 8001）

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

## Git 协作开发

项目采用 `main + develop + feature/*` 分支模型，提交规范为 Conventional Commits，详见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 质量保障

- 后端：`mvn test`（JUnit5）
- 前端：`npm run build`
- AI 服务：`pytest`
- CI：GitHub Actions 自动执行后端 / 前端 / AI 三路检查

## 更多文档

- [架构设计说明](docs/架构设计说明.md)
- [Docker 部署说明](deploy/README.md)
- [性能压测与实测数据](perf/README.md)

## 版本

- v4.0.0（2026-09-06）：可观测性 + 真实压测 + 限流熔断降级 + 分布式ID/幂等/本地消息表 + 检索升级
- v3.0.0（2026-08-30）：MQ 异步下单 + 订单状态机 + 缓存三防 + Rerank 精排 + 评估闭环 + 压测量化
- v2.0.0（2026-08-27）：AI 能力升级（多轮对话、SSE 流式、混合检索、Function Calling、问答缓存）+ 工程化（CI、Docker、Git 协作规范）
- v1.0.0（2026-08-15）：AI 基础能力（智能导购、文案生成、销售分析）
