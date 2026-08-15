# FastAPI AI 微服务

该服务为商品销售系统提供 AI 智能导购、商品文案生成和销售数据分析能力。Spring Boot 后端只调用本服务接口，不直接接触大模型 API。

## 启动

```powershell
cd "C:\Users\伊颖\Desktop\商品销售系统\源码\ai-service"
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python scripts\init_vector_store.py
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

## 模型配置

默认 `LLM_PROVIDER=mock`，不需要 API key，可以先跑通演示。

通义千问兼容模式：

```env
LLM_PROVIDER=dashscope
LLM_MODEL=qwen-plus
LLM_API_KEY=你的DashScopeKey
```

OpenAI：

```env
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini
LLM_API_KEY=你的OpenAIKey
```

智谱 AI：

```env
LLM_PROVIDER=zhipu
LLM_MODEL=glm-4-flash
LLM_API_KEY=你的智谱Key
```
