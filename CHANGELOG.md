# 更新日志

## [2.0.0] - 2026-08-29

### AI 能力升级（ai-service）
- 新增 **多轮对话上下文管理**：按 sessionId 维护会话记忆（LRU + TTL），支持连续追问。
- 检索升级为 **混合检索**：FAISS 向量 + BM25 关键词多路召回，RRF 融合 + 销量加权重排。
- 新增 **Function Calling Agent**：内置商品搜索 / 库存查询 / 热销查询工具，模型可主动调用。
- 新增 **问答结果缓存**：同 session 同问题命中缓存，降低 LLM 调用成本。
- 新增 **SSE 流式输出**：`/ai/guide/stream` 打字机式返回导购回答。
- 新增 pytest 单元测试（25 个用例，离线可跑）。
- 结构化日志：记录 AI 调用耗时 / Provider / 成功状态。

### 后端升级（springboot）
- 新增 **Redis 分布式锁**：订单支付防超卖（SETNX + Lua 释放 + 本地锁降级）。
- **两级缓存**：Caffeine 本地 + Redis 分布式（可切换）。
- 新增 **Swagger/OpenAPI** 接口文档（`/swagger-ui.html`）。
- 新增 **全局异常处理器**：统一异常 → Result，覆盖参数校验 / 业务异常 / 404 / 500。
- 新增 **AI 导购 SSE 代理端点**：前端直连后端即可获得流式能力。
- 新增单元测试：JUnit5 + Mockito + MockMvc（订单防超卖 / AI 网关 / 统一响应）。
- **修复编码问题**：全库统一 UTF-8，消除 GBK/UTF-8 混用导致的编译隐患。

### 前端升级（vue）
- AI 导购升级为 **多轮对话界面**：气泡消息 + 会话历史 + 清空会话。
- 接入 **SSE 流式打字机** 效果，流式失败自动回退普通模式。

### 工程化与协作
- 新增 **GitHub Actions CI**：后端 mvn test / 前端 build / AI pytest 三路并行。
- 新增 **Git 分支模型与协作规范**（CONTRIBUTING.md）：main + develop + feature/*。
- 新增 **Docker 容器化**：三服务 Dockerfile + docker-compose 一键编排 + K8s 清单。
- 新增 Issue / PR 模板、VSCode 调试配置。
- README 重写：架构图（Mermaid）、技术栈、快速开始。

## [1.0.0] - 2026-08

- 首个可用版本：电商基础业务 + AI 三大能力（智能导购 / 文案生成 / 销售分析）。
