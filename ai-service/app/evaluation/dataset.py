"""评估数据集：围绕商品智能导购场景构造的测试集（与 db_aps 真实商品对应）。

每条样本包含：
- query：用户问题
- expected_ids：期望被检索命中的商品 id（用于检索指标）
- reference_answer：参考回答关键词（用于生成指标启发式对比）
- category：场景类别

商品库为生鲜/农产品（24 款），期望 id 依据真实商品名称/库存/销量构造：
1 红富士苹果 / 2 有机胡萝卜 / 3 东北大米 / 4 云南松茸 / 5 有机生菜 / 6 海南芒果 /
7 陕西猕猴桃 / 8 广西百香果 / 9 山东大樱桃 / 10 山东大葱 / 11 四川青花椒 / 12 云南小黄姜 /
13 潍坊萝卜 / 14 黑龙江黄豆 / 15 湖南茶油 / 16 河南小麦粉 / 17 内蒙古小米 / 18 新疆红枣 /
19 西湖龙井 / 20 云南花椒 / 21 安徽毛豆 / 22 有机西红柿 / 23 有机菠菜 / 24 有机土豆
"""
from __future__ import annotations

from typing import Any, Dict, List

EVAL_DATASET: List[Dict[str, Any]] = [
    {
        "query": "想买点新鲜苹果，有没有推荐",
        "expected_ids": ["1", "9"],
        "reference_answer": "推荐红富士苹果、山东大樱桃等新鲜水果。",
        "category": "导购",
    },
    {
        "query": "有机蔬菜有哪些可以买",
        "expected_ids": ["2", "5", "22", "23", "24"],
        "reference_answer": "有机胡萝卜、有机生菜、有机西红柿、有机菠菜、有机土豆等。",
        "category": "导购",
    },
    {
        "query": "主食类的大米和小麦粉推荐",
        "expected_ids": ["3", "16"],
        "reference_answer": "东北大米、河南小麦粉等主食。",
        "category": "导购",
    },
    {
        "query": "云南产地的食材有什么",
        "expected_ids": ["4", "12", "20"],
        "reference_answer": "云南松茸、云南小黄姜、云南花椒等。",
        "category": "导购",
    },
    {
        "query": "芒果和猕猴桃这类水果",
        "expected_ids": ["6", "7"],
        "reference_answer": "海南芒果、陕西猕猴桃。",
        "category": "导购",
    },
    {
        "query": "花椒调味料哪个产地好",
        "expected_ids": ["11", "20"],
        "reference_answer": "四川青花椒、云南花椒。",
        "category": "导购",
    },
    {
        "query": "养生类的红枣和茶叶",
        "expected_ids": ["18", "19"],
        "reference_answer": "新疆红枣、西湖龙井。",
        "category": "导购",
    },
    {
        "query": "销量最高的热销商品是哪些",
        "expected_ids": ["7", "14", "13", "1", "24"],
        "reference_answer": "陕西猕猴桃、黑龙江黄豆、潍坊萝卜、红富士苹果、有机土豆销量领先。",
        "category": "销售分析",
    },
    {
        "query": "库存少需要补货的商品",
        "expected_ids": ["4", "7", "19"],
        "reference_answer": "云南松茸、陕西猕猴桃、西湖龙井库存较低，建议补货。",
        "category": "库存",
    },
    {
        "query": "东北产的大米和小米",
        "expected_ids": ["3", "17"],
        "reference_answer": "东北大米、内蒙古小米（北方杂粮）。",
        "category": "导购",
    },
]


def get_eval_dataset() -> List[Dict[str, Any]]:
    return EVAL_DATASET
