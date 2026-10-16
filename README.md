# AI 智能商品销售系统

一个在传统电商系统上扩展 AI 能力的全栈项目。系统由 Vue 前端、Spring Boot 后端、MySQL 数据库与独立 FastAPI AI 微服务组成。

## 功能模块

**电商业务**：商品浏览、分类搜索、购物车、收藏、下单支付、评价、物流跟踪；后台管理商品、分类、订单、库存、公告、轮播图、用户与角色权限；ECharts 销售统计可视化。

**AI 能力**：自然语言导购推荐、商品文案自动生成、销售数据分析、多轮对话上下文记忆、SSE 流式输出、向量语义检索、Function Calling 工具调用。

**高并发设计**：MQ 异步下单削峰、Redis 预扣库存防超卖、订单状态机流转、布隆过滤器防缓存穿透、互斥锁防击穿、TTL 抖动防雪崩、令牌桶限流、滑动窗口熔断。

## 系统架构

前端 Vue 运行在 8080 端口，负责用户端与后台管理页面。前端不直接调用 AI 服务，所有请求经过 Spring Boot 后端统一转发。

Spring Boot 后端运行在 1234 端口，负责业务逻辑、数据库操作、缓存、限流熔断，并作为网关调用 AI 微服务。

FastAPI AI 微服务运行在 8001 端口，独立部署，封装向量检索、大模型调用、工具执行与问答缓存。更换向量库或升级模型只需改动这个服务。

数据层使用 MySQL 存储业务数据，Redis 做缓存与分布式锁。

## 技术栈

- 前端：Vue 2、Element UI、Axios、ECharts
- 后端：Spring Boot 3.4、MyBatis-Plus、Spring Security、JWT、Redis
- AI 服务：Python、FastAPI、FAISS、Pydantic
- 数据库：MySQL、Redis

## 目录结构

```text
AI-Shopping-System/
├── springboot/     # Spring Boot 后端
├── vue/            # Vue 前端
├── ai-service/     # FastAPI AI 微服务
├── deploy/         # Docker 部署配置
├── docs/           # 架构设计说明
├── perf/           # 压测脚本
└── sql/            # 数据库脚本
```

## 本地启动

1. 启动 MySQL，导入 `sql/db_aps.sql`。
2. 启动 AI 微服务：`cd ai-service`，创建虚拟环境，安装依赖，运行 `uvicorn app.main:app --port 8001`。
3. 启动后端：`cd springboot`，运行 `mvn spring-boot:run`。
4. 启动前端：`cd vue`，运行 `npm install && npm run serve`。

访问 `http://localhost:8080`。
