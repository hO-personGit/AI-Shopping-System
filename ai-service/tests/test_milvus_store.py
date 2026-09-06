# -*- coding: utf-8 -*-
"""Milvus 适配层与向量库降级测试。

覆盖：
1. pymilvus 未安装时 MilvusStore 抛出 MilvusUnavailableError；
2. vector_db=milvus 但不可用时，ProductVectorStore 自动降级 FAISS；
3. 检索指标新增 hitRate@k 口径。
"""
import importlib.util

import pytest


def test_milvus_store_raises_when_client_missing():
    """pymilvus 未安装 → MilvusStore 构造抛 MilvusUnavailableError。"""
    if importlib.util.find_spec("pymilvus"):
        pytest.skip("pymilvus 已安装，跳过未安装降级用例")
    from app.services.milvus_store import MilvusStore, MilvusUnavailableError
    with pytest.raises(MilvusUnavailableError):
        MilvusStore(embedding_dim=384)


def test_vector_store_falls_back_to_faiss_when_milvus_unavailable(monkeypatch):
    """vector_db=milvus 但 Milvus 不可用 → 自动降级 FAISS。"""
    from app.config import settings
    from app.services.milvus_store import MilvusUnavailableError
    from app.services.vector_store import ProductVectorStore

    monkeypatch.setattr(settings, "vector_db", "milvus")

    store = ProductVectorStore()
    assert store.backend == "faiss", "Milvus 不可用时应降级为 faiss"
    assert store.milvus is None


def test_hit_rate_metric():
    """hitRate@k：命中任一相关商品即 1。"""
    from app.evaluation.metrics import hit_rate_at_k
    assert hit_rate_at_k(["1", "2"], ["3", "1", "4"], k=3) == 1.0
    assert hit_rate_at_k(["1", "2"], ["3", "4"], k=5) == 0.0
    assert hit_rate_at_k(["9"], ["9"], k=1) == 1.0


def test_retrieval_metrics_include_hit_rate():
    """retrieval_metrics 应包含 hitRate@k。"""
    from app.evaluation.metrics import retrieval_metrics
    metrics = retrieval_metrics(["1"], ["1", "2"], k=5)
    assert "hitRate@5" in metrics
    assert metrics["hitRate@5"] == 1.0
