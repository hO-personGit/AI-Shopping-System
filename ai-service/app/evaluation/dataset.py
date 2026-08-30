"""评估数据集：围绕商品智能导购场景构造的测试集。

每条样本包含：
- query：用户问题
- expected_ids：期望被检索命中的商品 id（用于检索指标）
- reference_answer：参考回答关键词（用于生成指标启发式对比）

注意：expected_ids 需要与数据库中的商品 id 对应。示例数据以 id=1..10 的商品为例，
实际运行前可依据 db_aps 商品表调整（也可只跑检索指标，忽略生成指标）。
"""
from __future__ import annotations

from typing import Any, Dict, List

EVAL_DATASET: List[Dict[str, Any]] = [
    {
        "query": "有没有性价比高的运动鞋",
        "expected_ids": ["1", "2", "3"],
        "reference_answer": "推荐价格合适、销量较好的运动鞋，可按评价和使用场景比较。",
        "category": "导购",
    },
    {
        "query": "推荐一款适合办公用的电脑",
        "expected_ids": ["4", "5"],
        "reference_answer": "推荐办公笔记本，关注内存、轻薄与续航。",
        "category": "导购",
    },
    {
        "query": "夏季空调什么牌子卖得好",
        "expected_ids": ["6", "7", "8"],
        "reference_answer": "推荐销量领先的空调品牌，结合匹数与能效选择。",
        "category": "导购",
    },
    {
        "query": "我需要补货量大的热销商品",
        "expected_ids": ["9", "10"],
        "reference_answer": "热销且库存偏低的商品建议优先补货。",
        "category": "销售分析",
    },
    {
        "query": "库存还剩多少的爆款商品",
        "expected_ids": ["2", "6"],
        "reference_answer": "查询库存充足的爆款商品供选购。",
        "category": "导购",
    },
]


def get_eval_dataset() -> List[Dict[str, Any]]:
    return EVAL_DATASET
