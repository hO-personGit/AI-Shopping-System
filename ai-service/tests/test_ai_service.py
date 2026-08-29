"""AI 业务服务单元测试：导购 / 文案 / 销售分析 / 工具调用（mock 模式离线运行）。"""
import pytest


def test_smart_guide_returns_structured_result(monkeypatch, fake_products):
    from app.services.ai_service import ProductAIService
    from app.services.vector_store import ProductVectorStore

    # 离线桩：替换混合检索为固定候选
    store = ProductVectorStore()
    monkeypatch.setattr(store, "hybrid_search",
                        lambda q, k, **kw: [store._product_to_metadata(p) | {"score": 0.9} for p in fake_products[:2]])
    monkeypatch.setattr("app.services.ai_service.product_vector_store", store)

    svc = ProductAIService()
    from app.schemas import GuideRequest
    resp = svc.smart_guide(GuideRequest(query="推荐适合学生的高性价比商品", userId=1))
    assert resp.answer
    assert len(resp.recommendations) >= 1
    assert resp.source == "mock"
    # 多轮记忆已写入
    assert svc.smart_guide(GuideRequest(query="推荐适合学生的高性价比商品", userId=1)).cached is True


def test_guide_caches_same_query(monkeypatch, fake_products):
    from app.services.ai_service import ProductAIService
    from app.services.cache import answer_cache
    from app.services.vector_store import ProductVectorStore
    from app.schemas import GuideRequest

    answer_cache.clear()
    store = ProductVectorStore()
    monkeypatch.setattr(store, "hybrid_search",
                        lambda q, k, **kw: [store._product_to_metadata(fake_products[0]) | {"score": 0.9}])
    monkeypatch.setattr("app.services.ai_service.product_vector_store", store)
    svc = ProductAIService()
    r1 = svc.smart_guide(GuideRequest(query="推荐手机", sessionId="sess-cache"))
    r2 = svc.smart_guide(GuideRequest(query="推荐手机", sessionId="sess-cache"))
    assert r2.cached is True
    assert r1.answer == r2.answer


def test_rule_tool_route_stock(monkeypatch, fake_products):
    from app.services.ai_service import ProductAIService
    from app.schemas import GuideRequest
    svc = ProductAIService()
    calls = svc._run_tools("这款还有库存吗", [], [{"id": 1, "name": "小米手环"}])
    assert calls and calls[0]["name"] == "query_stock"
    assert "stock" in calls[0]["output"]


def test_rule_tool_route_top_selling(monkeypatch, fake_products):
    from app.services.ai_service import ProductAIService
    svc = ProductAIService()
    calls = svc._run_tools("卖得最好的商品是哪些", [], [])
    assert calls and calls[0]["name"] == "get_top_selling"
    assert calls[0]["output"]["products"][0]["name"] == "联想蓝牙耳机"  # 销量最高


def test_generate_copywriting(monkeypatch):
    from app.services.ai_service import ProductAIService
    from app.schemas import CopywritingRequest
    svc = ProductAIService()
    resp = svc.generate_copywriting(CopywritingRequest(name="小米手环", category="数码产品", price=249))
    assert resp.title
    assert resp.summary
    assert resp.detail
    assert resp.slogan


def test_analyze_sales(monkeypatch):
    from app.services.ai_service import ProductAIService
    from app.schemas import SalesAnalysisRequest
    svc = ProductAIService()
    resp = svc.analyze_sales(SalesAnalysisRequest(salesData={
        "topProducts": {"topProducts": [{"name": "小米手环"}]},
        "lowStockProducts": [{"name": "联想蓝牙耳机", "stock": 8}],
    }))
    assert resp.hot_products_analysis
    assert resp.stock_warning
    assert resp.replenishment_advice
    assert resp.summary
