"""RAG 评估指标单元测试（RAGAS 风格）。"""
import pytest
from app.evaluation.metrics import (aggregate_metrics, answer_relevance_score,
                                    context_precision, mrr, ndcg_at_k,
                                    precision_at_k, recall_at_k,
                                    retrieval_metrics, source_overlap_score)


def test_recall_at_k():
    assert recall_at_k(["1", "2"], ["1", "3", "4"], 3) == 0.5
    assert recall_at_k(["1", "2"], ["1", "2", "3"], 3) == 1.0
    assert recall_at_k(["1"], ["2"], 5) == 0.0
    assert recall_at_k([], ["1"], 5) == 0.0


def test_precision_at_k():
    assert precision_at_k(["1", "2"], ["1", "3", "4"], 3) == 0.3333
    assert precision_at_k(["1"], ["1", "2"], 2) == 0.5
    assert precision_at_k(["1"], [], 5) == 0.0


def test_mrr():
    assert mrr(["1", "2"], ["3", "1"]) == 0.5  # 第一个相关在 rank2 → 1/2
    assert mrr(["1"], ["2", "3"]) == 0.0
    assert mrr(["1"], ["1"]) == 1.0


def test_ndcg_at_k():
    # 相关性排序 [1,2]，retrieved [1,2,3] → 完美排序 NDCG=1
    assert ndcg_at_k(["1", "2"], ["1", "2", "3"], 5) == 1.0
    # 相关排后位 NDCG < 1
    val = ndcg_at_k(["1"], ["3", "1"], 5)
    assert 0 < val < 1.0


def test_retrieval_metrics_composite():
    metrics = retrieval_metrics(["1", "2"], ["1", "3", "2"], 5)
    assert metrics["recall@5"] == 1.0
    assert metrics["mrr"] == 1.0
    assert "precision@5" in metrics and "ndcg@5" in metrics


def test_source_overlap_score():
    names = ["运动跑步鞋", "商务办公电脑"]
    answer = "为你推荐运动跑步鞋，它销量不错"
    assert source_overlap_score(answer, names) == 0.5
    assert source_overlap_score("完全不相关", names) == 0.0
    assert source_overlap_score("", names) == 0.0


def test_answer_relevance_score():
    assert answer_relevance_score("推荐运动鞋", "推荐这款运动鞋，舒适透气") > 0
    assert answer_relevance_score("推荐运动鞋", "这是电脑评测") == 0.0


def test_context_precision():
    assert context_precision(["1"], ["1", "2", "3"]) == 0.3333


def test_aggregate_metrics():
    results = [
        {"recall@5": 1.0, "mrr": 0.5},
        {"recall@5": 0.5, "mrr": 1.0},
    ]
    agg = aggregate_metrics(results)
    assert agg["recall@5"] == 0.75
    assert agg["mrr"] == 0.75
