import json
from typing import Any, Dict, List


def guide_prompt(query: str, candidates: List[Dict[str, Any]]) -> str:
    return f"""
用户购物需求：{query}
候选商品数据如下：
{json.dumps(candidates, ensure_ascii=False, indent=2)}

请结合商品名称、分类、价格、库存、销量、评价与描述，筛选最适合的商品。
只输出 JSON：
{{
  "answer": "自然语言导购解说，说明选择逻辑",
  "recommendations": [
    {{"id": 1, "reason": "推荐理由"}}
  ]
}}
"""


def copywriting_prompt(product: Dict[str, Any]) -> str:
    return f"""
请为商品生成后台可直接使用的销售文案。
商品基础信息：
{json.dumps(product, ensure_ascii=False, indent=2)}

只输出 JSON：
{{
  "title": "商品标题，简洁有卖点",
  "summary": "商品简介，60字以内",
  "detail": "商品详情描述，分段说明卖点、适用场景、购买理由",
  "slogan": "营销推荐语，20字以内"
}}
"""


def sales_analysis_prompt(sales_data: Dict[str, Any]) -> str:
    return f"""
你是电商运营分析师。请基于以下商品销售系统数据进行经营分析：
{json.dumps(sales_data, ensure_ascii=False, indent=2)}

请只输出 JSON：
{{
  "hotProductsAnalysis": "热销商品分析",
  "stockWarning": "库存预警建议",
  "replenishmentAdvice": "补货建议",
  "salesTrendSummary": "销售趋势总结",
  "summary": "面向管理者的总体结论"
}}
"""
