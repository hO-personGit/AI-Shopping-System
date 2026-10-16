# AI 智能商品销售系统 · AI 微服务

基于 **Python + FastAPI + LangChain + FAISS** 的独立 AI 服务，为前端/后端提供三大 AI 能力，并支持多轮对话、混合检索、Function Calling Agent、SSE 流式输出与问答缓存。

## 能力清单

| 能力 | 说明 |
|---|---|
| 智能导购 `/ai/guide` | 自然语言 → 混合检索商品 → 大模型生成推荐 |
| 流式导购 `/ai/guide/stream` | SSE 打字机式输出导购回答 |
| 文案生成 `/ai/copywriting` | 商品标题 / 简介 / 详情 / 营销语 |
| 销售分析 `/ai/sales-analysis` | 热销 / 库存预警 / 补货建议 |
| 多轮对话 | 按 sessionId 维护上下文（LRU + TTL） |
| 混合检索 | 向量 + 关键词多路召回，RRF 融合重排 |
| Function Calling Agent | 商品搜索 / 库存查询 / 热销查询工具 |
| 问答缓存 | 同 session 同问题直接命中，降本提速 |

## 目录结构

```text
ai-service/
├── app/
│   ├── main.py              # FastAPI 入口（含 SSE 流式）
│   ├── config.py            # 配置（pydantic-settings）
│   ├── schemas.py           # 请求/响应模型
│   └── services/
│       ├── ai_service.py    # 业务编排（导购/文案/分析）
│       ├── vector_store.py  # FAISS 向量库 + 混合检索
│       ├── llm_client.py    # 大模型客户端（多 Provider + 流式 + Function Calling）
│       ├── memory.py        # 多轮对话记忆
│       ├── cache.py         # 问答结果缓存
│       ├── tools.py         # Function Calling 工具集
│       ├── prompts.py       # Prompt 工程
│       └── db.py            # MySQL 数据访问
├── tests/                   # pytest 单元测试（离线可跑）
├── scripts/init_vector_store.py
├── requirements.txt
├── Dockerfile
└── .env.example
```

## 启动

```bash
python -m venv .venv
.\.venv\Scripts\activate        # Windows
pip install -r requirements.txt
copy .env.example .env
python scripts\init_vector_store.py
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

## 配置（.env）

- `LLM_PROVIDER=mock`：无需 Key，离线演示；`dashscope` / `openai` / `zhipu` 接真实大模型。
- `RETRIEVAL_MODE=hybrid`：向量+关键词多路召回；可切换 `vector` / `keyword`。
- `CACHE_TTL_SECONDS` / `MEMORY_MAX_TURNS`：缓存与记忆参数。

## 测试

```bash
pip install pytest
python -m pytest tests -v
```

测试全部离线运行（Mock 数据 + mock LLM），无需 MySQL 与大模型 Key。

## Docker

```bash
docker build -t ai-shopping-ai:2.0.0 .
docker run -d -p 8001:8001 --env-file .env ai-shopping-ai:2.0.0
```

## 接口速览

- `GET /health` 健康检查
- `POST /ai/guide` 智能导购
- `POST /ai/guide/stream` SSE 流式导购
- `POST /ai/copywriting` 文案生成
- `POST /ai/sales-analysis` 销售分析
- `POST /ai/chat/clear?session_id=xxx` 清空会话
- `POST /ai/rebuild-products-index` 重建商品向量索引
