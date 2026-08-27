"""混合检索（关键词多路召回 + RRF 融合）单元测试。"""
import pytest


def test_keyword_search_finds_relevant(fake_products):
    from app.services.vector_store import ProductVectorStore
    store = ProductVectorStore()
    hits = store.keyword_search("蓝牙耳机", top_k=3)
    assert hits, "关键词检索应返回结果"
    assert hits[0]["id"] == 2, "名称包含'耳机'的商品应排在最前"


def test_keyword_search_relevance_order(fake_products):
    from app.services.vector_store import ProductVectorStore
    store = ProductVectorStore()
    # 多词命中 + 名称命中优先级验证
    hits = store.keyword_search("智能手环 运动", top_k=3)
    assert hits[0]["id"] == 1
    assert all(h["score"] >= 0 for h in hits)


def test_hybrid_search_returns_merged(fake_products):
    from app.services.vector_store import ProductVectorStore
    store = ProductVectorStore()
    hits = store.hybrid_search("耳机", top_k=3, sales_boost=True)
    assert len(hits) >= 1
    assert "score" in hits[0]
    assert "rrfScore" in hits[0]
    # 排序稳定
    scores = [h["score"] for h in hits]
    assert scores == sorted(scores, reverse=True)


def test_hybrid_search_top_k_limit(fake_products):
    from app.services.vector_store import ProductVectorStore
    store = ProductVectorStore()
    hits = store.hybrid_search("商品", top_k=2, sales_boost=False)
    assert len(hits) <= 2
