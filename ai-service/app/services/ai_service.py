from typing import Any, Dict, List

from app.config import settings
from app.schemas import CopywritingRequest, CopywritingResponse, GuideRequest, GuideResponse, ProductRecommendation, SalesAnalysisRequest, SalesAnalysisResponse
from app.services.db import ProductRepository
from app.services.llm_client import llm_client
from app.services.prompts import copywriting_prompt, guide_prompt, sales_analysis_prompt
from app.services.vector_store import product_vector_store


class ProductAIService:
    def __init__(self):
        self.repository = ProductRepository()

    def smart_guide(self, request: GuideRequest) -> GuideResponse:
        candidates = product_vector_store.search(request.query, request.top_k)
        fallback = self._guide_fallback(request.query, candidates)
        prompt = guide_prompt(request.query, candidates)
        response = llm_client.generate_json(prompt, fallback)
        merged = self._merge_recommendations(response.get("recommendations", []), candidates)
        result = GuideResponse(
            answer=response.get("answer") or fallback["answer"],
            recommendations=merged,
            source=settings.llm_provider,
        )
        self.repository.log_ai_call("AI智能导购", request.model_dump(by_alias=True), result.model_dump(by_alias=True), settings.llm_provider, True)
        return result

    def generate_copywriting(self, request: CopywritingRequest) -> CopywritingResponse:
        product = request.model_dump(by_alias=True)
        fallback = self._copywriting_fallback(product)
        response = llm_client.generate_json(copywriting_prompt(product), fallback)
        result = CopywritingResponse(
            title=response.get("title") or fallback["title"],
            summary=response.get("summary") or fallback["summary"],
            detail=response.get("detail") or fallback["detail"],
            slogan=response.get("slogan") or fallback["slogan"],
            source=settings.llm_provider,
        )
        self.repository.log_ai_call("AI商品文案生成", product, result.model_dump(), settings.llm_provider, True)
        return result

    def analyze_sales(self, request: SalesAnalysisRequest) -> SalesAnalysisResponse:
        sales_data = request.sales_data or {}
        fallback = self._sales_fallback(sales_data)
        response = llm_client.generate_json(sales_analysis_prompt(sales_data), fallback)
        result = SalesAnalysisResponse(
            hotProductsAnalysis=response.get("hotProductsAnalysis") or fallback["hotProductsAnalysis"],
            stockWarning=response.get("stockWarning") or fallback["stockWarning"],
            replenishmentAdvice=response.get("replenishmentAdvice") or fallback["replenishmentAdvice"],
            salesTrendSummary=response.get("salesTrendSummary") or fallback["salesTrendSummary"],
            summary=response.get("summary") or fallback["summary"],
            source=settings.llm_provider,
        )
        self.repository.log_ai_call("AI销售分析", sales_data, result.model_dump(by_alias=True), settings.llm_provider, True)
        return result

    def _guide_fallback(self, query: str, candidates: List[Dict[str, Any]]) -> Dict[str, Any]:
        names = "、".join([item.get("name", "") for item in candidates[:3]]) or "当前在售商品"
        return {
            "answer": f"根据“{query}”的需求，优先为你筛选了价格合适、库存充足、销量表现较好的商品：{names}。你可以优先比较价格、评价和使用场景。",
            "recommendations": [
                {"id": item.get("id"), "reason": self._recommend_reason(item)} for item in candidates
            ],
        }

    def _merge_recommendations(self, llm_items: List[Dict[str, Any]], candidates: List[Dict[str, Any]]) -> List[ProductRecommendation]:
        reason_map = {str(item.get("id")): item.get("reason", "") for item in llm_items if isinstance(item, dict)}
        result = []
        for item in candidates:
            reason = reason_map.get(str(item.get("id"))) or self._recommend_reason(item)
            result.append(ProductRecommendation(
                id=item.get("id"),
                name=item.get("name", ""),
                category=item.get("category", ""),
                price=item.get("price", 0),
                stock=item.get("stock", 0),
                salesCount=item.get("salesCount", 0),
                reason=reason,
                score=item.get("score", 0),
            ))
        return result

    def _recommend_reason(self, item: Dict[str, Any]) -> str:
        return f"{item.get('category', '商品')}品类，价格约{item.get('price', 0)}元，库存{item.get('stock', 0)}件，销量{item.get('salesCount', 0)}，适合优先对比。"

    def _copywriting_fallback(self, product: Dict[str, Any]) -> Dict[str, Any]:
        name = product.get("name") or product.get("category") or "精选商品"
        category = product.get("category") or "品质好物"
        price = product.get("price")
        origin = product.get("placeOfOrigin") or product.get("place_of_origin") or "优选产地"
        title = f"{name} 高性价比{category}"
        summary = f"甄选{origin}好物，兼顾品质、价格与日常使用体验。"
        if price:
            summary += f" 当前参考价约{price}元。"
        detail = f"【商品亮点】{name}定位为{category}，适合日常购买与家庭使用。\n【品质说明】来源于{origin}，强调稳定供应与实用体验。\n【购买建议】适合关注性价比、品质和便捷购物的用户。"
        return {"title": title, "summary": summary[:80], "detail": detail, "slogan": "品质好物，放心选购"}

    def _sales_fallback(self, sales_data: Dict[str, Any]) -> Dict[str, Any]:
        top_products = sales_data.get("topProducts", {}).get("topProducts", []) if isinstance(sales_data.get("topProducts"), dict) else sales_data.get("topProducts", [])
        low_stock = sales_data.get("lowStockProducts", [])
        hot_name = top_products[0].get("name") if top_products else "暂无明显爆款"
        return {
            "hotProductsAnalysis": f"当前热销商品以“{hot_name}”为代表，建议继续观察其销量贡献和关联品类带动效果。",
            "stockWarning": f"低库存商品数量为{len(low_stock)}个，需优先检查库存低且仍有销量的商品。",
            "replenishmentAdvice": "建议按销量排序制定补货优先级：热销且低库存商品优先补货，滞销商品谨慎补货。",
            "salesTrendSummary": "结合月度订单和销售额变化，重点关注增长率、客单价和品类结构是否稳定。",
            "summary": "当前经营分析已完成，可将热销商品运营、库存预警和补货计划作为下一步重点。",
        }


product_ai_service = ProductAIService()
