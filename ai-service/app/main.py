"""FastAPI AI 微服务入口：智能导购、文案生成、销售分析、SSE 流式输出。"""
from __future__ import annotations

import json
import logging
import time

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

from app.config import settings
from app.schemas import (CopywritingRequest, CopywritingResponse, GuideRequest,
                         GuideResponse, SalesAnalysisRequest,
                         SalesAnalysisResponse)
from app.services.ai_service import product_ai_service
from app.services.vector_store import product_vector_store

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

app = FastAPI(title=settings.app_name, version="2.0.0", description="AI 智能商品销售系统 AI 微服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "service": settings.app_name, "provider": settings.llm_provider, "version": "2.0.0"}


@app.post("/ai/rebuild-products-index")
def rebuild_products_index():
    try:
        count = product_vector_store.build(force=True)
        return {"status": "ok", "indexedProducts": count}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"商品向量库初始化失败：{exc}")


@app.post("/ai/guide", response_model=GuideResponse)
def smart_guide(request: GuideRequest):
    try:
        return product_ai_service.smart_guide(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"AI智能导购失败：{exc}")


@app.post("/ai/guide/stream")
async def smart_guide_stream(request: GuideRequest):
    """SSE 流式智能导购：打字机式逐字返回导购回答。"""
    def event_generator():
        try:
            result = product_ai_service.smart_guide(request)
            answer = result.answer
            # 先逐块推送导购回答（打字机效果）
            for i in range(0, len(answer), 24):
                yield f"data: {json.dumps({'delta': answer[i:i + 24]}, ensure_ascii=False)}\n\n"
            # 最后推送结构化数据（推荐列表、工具调用、来源）
            payload = {
                "done": True,
                "source": result.source,
                "toolCalls": result.tool_calls,
                "recommendations": [r.model_dump(by_alias=True) for r in result.recommendations],
            }
            yield f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
        except Exception as exc:
            yield f"data: {json.dumps({'error': str(exc)}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/ai/copywriting", response_model=CopywritingResponse)
def copywriting(request: CopywritingRequest):
    try:
        return product_ai_service.generate_copywriting(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"AI商品文案生成失败：{exc}")


@app.post("/ai/sales-analysis", response_model=SalesAnalysisResponse)
def sales_analysis(request: SalesAnalysisRequest):
    try:
        return product_ai_service.analyze_sales(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"AI销售分析失败：{exc}")


@app.post("/ai/chat/clear")
def clear_session(session_id: str = ""):
    """清空指定会话上下文。"""
    from app.services.memory import memory
    if not session_id:
        raise HTTPException(status_code=400, detail="sessionId 不能为空")
    memory.clear(session_id)
    return {"status": "ok", "cleared": True}
