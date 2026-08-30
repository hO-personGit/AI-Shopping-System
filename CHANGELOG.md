# 更新日志

## [3.0.0] - 2026-08-30

### MQ 异步下单链路 + 订单状态机（springboot）
- 引入 **RabbitMQ**：下单 → Redis 预扣库存（Lua 原子防超卖）→ MQ 异步落库（削峰解耦）→ 延迟队列超时关单。
- 新增 **订单状态机** `OrderStatus`：0 待支付 → 1 已支付 → 2 已发货 → 3 已完成 / 4 已取消 / 5 退款中 → 6 已退款 / 7 退款失败，非法流转统一拦截。
- 新增 **延迟队列 + 死信交换机**（TTL + DLX）实现「下单 N 分钟未支付自动关单」，并叠加**定时任务兜底扫描**保证最终一致性。
- 消费者**幂等处理 + 手动 ACK**，支付成功 / 取消 / 退款事件异步解耦；RabbitMQ 不可用时**自动降级本地同步**，演示环境无 MQ 也可运行。
- 订单表新增 `order_no`（业务订单号 / 幂等键），迁移脚本 `数据库/upgrade/v3.0.0_mq_async_order.sql`。

### 缓存三防（穿透 / 击穿 / 雪崩）
- 新增 `ProductCacheService`：**布隆过滤器**拦截穿透 + **空值短 TTL 缓存**兜底；**互斥锁双检**重建防击穿；**TTL 随机抖动**防雪崩。
- 商品详情读取升级为 Caffeine 一级 + Redis 二级两级缓存（可开关），写操作自动失效缓存。
- 商品库存扣减升级为 **DB 原子 UPDATE**（WHERE stock >= quantity），支付防超卖兜底。

### AI 链路精排 + 评估闭环（ai-service）
- 新增 **Rerank 语义精排**：RRF 融合后二次排序，支持 lexical（无依赖）/ api（接 bge-reranker 服务）双模式，失败自动降级。
- 新增 **RAGAS 风格评估** `app/evaluation`：recall@k / precision@k / MRR / NDCG@k + 忠实度 / 相关性代理指标，
  一键产出 `eval_report/eval_report.md` 评估报告，形成「检索-生成-评估」闭环。

### 性能压测量化（perf）
- 新增 JMeter 测试计划（商品读取缓存 / 列表 / 下单支付并发防超卖）。
- 新增 Python 轻量压测脚本（QPS / P50 / P95 / P99 / 成功率 / 超卖检查）。
- 新增 `perf/README.md`：指标口径与结果记录模板，量化缓存 / MQ / 并发收益。

### 测试
- 新增订单状态机 / 消息处理器幂等 / 缓存 / Rerank / 评估指标等测试；后端 + AI 全量测试通过。

## [2.0.0] - 2026-08-29

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
