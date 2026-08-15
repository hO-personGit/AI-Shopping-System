from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, ConfigDict


class ProductRecommendation(BaseModel):
    id: Optional[int] = None
    name: str = ""
    category: str = ""
    price: float = 0
    stock: int = 0
    sales_count: int = Field(default=0, alias="salesCount")
    reason: str = ""
    score: float = 0

    model_config = ConfigDict(populate_by_name=True)


class GuideRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=500)
    user_id: Optional[int] = Field(default=None, alias="userId")
    top_k: int = Field(default=5, ge=1, le=10, alias="topK")

    model_config = ConfigDict(populate_by_name=True)


class GuideResponse(BaseModel):
    answer: str
    recommendations: List[ProductRecommendation]
    source: str = ""


class CopywritingRequest(BaseModel):
    name: str = ""
    category: str = ""
    price: Optional[float] = None
    stock: Optional[int] = None
    place_of_origin: str = Field(default="", alias="placeOfOrigin")
    description: str = ""
    selling_points: str = Field(default="", alias="sellingPoints")

    model_config = ConfigDict(populate_by_name=True)


class CopywritingResponse(BaseModel):
    title: str
    summary: str
    detail: str
    slogan: str
    source: str = ""


class SalesAnalysisRequest(BaseModel):
    sales_data: Dict[str, Any] = Field(default_factory=dict, alias="salesData")

    model_config = ConfigDict(populate_by_name=True)


class SalesAnalysisResponse(BaseModel):
    hot_products_analysis: str = Field(alias="hotProductsAnalysis")
    stock_warning: str = Field(alias="stockWarning")
    replenishment_advice: str = Field(alias="replenishmentAdvice")
    sales_trend_summary: str = Field(alias="salesTrendSummary")
    summary: str
    source: str = ""

    model_config = ConfigDict(populate_by_name=True)
