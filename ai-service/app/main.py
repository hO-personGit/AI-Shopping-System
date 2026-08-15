from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import CopywritingRequest, CopywritingResponse, GuideRequest, GuideResponse, SalesAnalysisRequest, SalesAnalysisResponse
from app.services.ai_service import product_ai_service
from app.services.vector_store import product_vector_store

app = FastAPI(title=settings.app_name, version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "service": settings.app_name, "provider": settings.llm_provider}


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
