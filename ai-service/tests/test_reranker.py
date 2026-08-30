"""Rerank 语义精排单元测试。"""
from app.services.reranker import Reranker


def _candidates():
    return [
        {"id": 1, "name": "运动跑步鞋 透气轻便", "category": "运动鞋", "description": "适合跑步健身", "score": 0.8},
        {"id": 2, "name": "商务办公电脑 轻薄本", "category": "电脑", "description": "办公高性能", "score": 0.6},
        {"id": 3, "name": "夏季空调 变频节能", "category": "空调", "description": "制冷快", "score": 0.7},
    ]


def test_lexical_rerank_reorders_by_query_overlap():
    r = Reranker(mode="lexical")
    result = r.rerank("推荐跑鞋", _candidates(), top_n=3)
    # 名称含「跑鞋」语义词命中多的应排前
    assert result[0]["id"] == 1
    assert "rerankScore" in result[0]
    assert result[0]["rerankSource"] == "lexical"


def test_rerank_none_keeps_order():
    r = Reranker(mode="none")
    result = r.rerank("推荐跑鞋", _candidates(), top_n=3)
    assert result == _candidates()


def test_rerank_top_n_limits():
    r = Reranker(mode="lexical")
    result = r.rerank("推荐跑鞋", _candidates(), top_n=1)
    assert len(result) == 1


def test_rerank_empty_candidates():
    r = Reranker(mode="lexical")
    assert r.rerank("查询", []) == []


def test_api_rerank_falls_back_to_lexical_on_error():
    r = Reranker(mode="api", base_url="http://invalid-host.invalid", api_key="k")
    result = r.rerank("推荐跑鞋", _candidates(), top_n=3)
    # 网络失败应降级 lexical 并返回结果
    assert result
    assert result[0]["rerankSource"] == "lexical"
