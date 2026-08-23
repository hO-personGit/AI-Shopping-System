# 基于 Python 3.11 构建 AI 微服务镜像
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 先装依赖（利用层缓存，代码变更不触发重复装依赖）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app ./app
COPY scripts ./scripts
COPY .env.example ./.env.example

# 向量库数据目录（运行时若缺失会自动从 DB 构建，可挂载卷持久化）
RUN mkdir -p /app/data/product_faiss

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
