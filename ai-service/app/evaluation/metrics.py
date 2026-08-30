"""RAG 检索与生成评估指标（RAGAS 风格）。

提供两类指标：
1. 检索质量（无需 LLM）：recall@k、precision@k、MRR、NDCG@k。
2. 生成质量：
   - 启发式（无需 LLM）：基于答案与检索上下文/query 的重合度，作为
     faithfulness / answer_relevancy 的代理指标，适合无 key 演示环境；
   - 大模型判定（可选）：配置真实 LLM 时，用 Prompt 让模型打分。
"""
from __future__ import annotations

import math
from typing import Any, Dict, List, Optional


# ================= 检索指标 =================

def recall_at_k(expected: List[str], retrieved: List[str], k: Optional[int] = None) -> float:
    if k is not None:
        retrieved = retrieved[:k]
    if not expected:
        return 0.0
    hit = len(set(expected) & set(retrieved))
    return round(hit / len(expected), 4)


def precision_at_k(expected: List[str], retrieved: List[str], k: Optional[int] = None) -> float:
    if k is not None:
        retrieved = retrieved[:k]
    if not retrieved:
        return 0.0
    hit = len(set(expected) & set(retrieved))
    return round(hit / len(retrieved), 4)


def mrr(expected: List[str], retrieved: List[str]) -> float:
    """第一个相关结果排名的倒数。"""
    expected_set = set(expected)
    for idx, rid in enumerate(retrieved):
        if rid in expected_set:
            return round(1.0 / (idx + 1), 4)
    return 0.0


def ndcg_at_k(expected: List[str], retrieved: List[str], k: int = 5) -> float:
    """NDCG@k（二元相关性）。"""
    expected_set = set(expected)
    if not expected_set:
        return 0.0
    dcg = 0.0
    for idx, rid in enumerate(retrieved[:k]):
        if rid in expected_set:
            dcg += 1.0 / math.log2(idx + 2)
    # 理想排序：前 min(k, |expected|) 位全是相关
    idcg = sum(1.0 / math.log2(i + 2) for i in range(min(k, len(expected_set))))
    if idcg <= 0:
        return 0.0
    return round(dcg / idcg, 4)


def retrieval_metrics(expected: List[str], retrieved: List[str], k: int = 5) -> Dict[str, float]:
    """聚合检索指标。expected/retrieved 均为商品 id 字符串列表。"""
    return {
        f"recall@{k}": recall_at_k(expected, retrieved, k),
        f"precision@{k}": precision_at_k(expected, retrieved, k),
        "mrr": mrr(expected, retrieved),
        f"ndcg@{k}": ndcg_at_k(expected, retrieved, k),
    }


# ================= 生成指标（启发式代理） =================

def _normalize(text: str) -> str:
    return (text or "").lower().replace(" ", "").replace("，", "").replace("。", "")


def source_overlap_score(answer: str, retrieved_names: List[str]) -> float:
    """忠实度代理：答案中命中检索到的商品名称的比例。

    检索结果里商品名被回答引用的越多，说明回答越贴近检索上下文（幻觉风险越低）。
    """
    if not retrieved_names or not answer:
        return 0.0
    ans = _normalize(answer)
    hit = sum(1 for name in retrieved_names if name and _normalize(name) in ans)
    return round(hit / len(retrieved_names), 4)


def answer_relevance_score(question: str, answer: str) -> float:
    """相关性代理：query 关键词在答案中的覆盖比例。"""
    if not question or not answer:
        return 0.0
    q = _normalize(question)
    a = _normalize(answer)
    # 中文场景按字符粒度计算 query 关键词在答案中的覆盖比例（无需分词器）
    tokens = [ch for ch in q if not ch.isascii()]
    if not tokens:
        return 0.0
    hit = sum(1 for t in tokens if t in a)
    return round(hit / len(tokens), 4)


def context_precision(expected: List[str], retrieved: List[str]) -> float:
    """上下文精度：检索结果中相关商品占比（等价 precision@k 全量）。"""
    return precision_at_k(expected, retrieved, len(retrieved))


# ================= 大模型判定（可选，RAGAS 风格） =================

def llm_faithfulness(question: str, context: str, answer: str, llm_generate) -> float:
    """让 LLM 判断 answer 是否被 context 支持（faithfulness），返回 0~1。

    llm_generate: callable(prompt) -> str，未配置真实 LLM 时返回启发式代理。
    """
    if llm_generate is None:
        return source_overlap_score(answer, [])
    prompt = (
        "你是严谨的评估器。仅根据给定【上下文】判断【回答】中的每个事实是否都有依据，"
        "输出一个 0~1 的小数（1=完全有依据，0=完全无依据，可参考 RAGAS faithfulness）。\n"
        f"上下文：{context}\n回答：{answer}\n请只输出小数。"
    )
    try:
        text = llm_generate(prompt).strip()
        val = float(text)
        return max(0.0, min(1.0, val))
    except Exception:
        return source_overlap_score(answer, [])


def aggregate_metrics(results: List[Dict[str, Any]]) -> Dict[str, float]:
    """对多条评估结果求平均。"""
    if not results:
        return {}
    keys = list(results[0].keys())
    agg: Dict[str, float] = {}
    for key in keys:
        vals = [r[key] for r in results if isinstance(r.get(key), (int, float))]
        if vals:
            agg[key] = round(sum(vals) / len(vals), 4)
    return agg
