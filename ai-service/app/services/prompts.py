"""提示词工程：约束输出、抑制幻觉、注入上下文与工具结果。"""
from __future__ import annotations

import json
from typing import Any, Dict, List


def _history_to_text(history: List[Dict[str, str]]) -> str:
    if not history:
        return "（暂无历史对话）"
    lines = [f"{'用户' if h['role'] == 'user' else '助手'}：{h['content']}" for h in history]
    return "\n".join(lines)


def guide_prompt(query: str, candidates: List[Dict[str, Any]],
                 history: List[Dict[str, str]] = None,
                 tool_outputs: str = "") -> str:
    """智能导购 Prompt：结合候选商品、多轮历史与工具结果生成推荐。"""
    history_text = _history_to_text(history or [])
    tool_text = tool_outputs or "（无需额外工具调用）"
    return f"""
你是电商平台的资深智能导购，只依据提供的商品数据作答，不要编造不存在的商品、价格或库存（禁止幻觉）。

【对话历史】
{history_text}

【本次用户需求】{query}

【候选商品数据】
{json.dumps(candidates, ensure_ascii=False, indent=2)}

【工具查询结果（可选参考）】
{tool_text}

请结合商品名称、分类、价格、库存、销量、评价与描述，筛选最适合的商品。
只输出 JSON（不要输出其他内容）：
{{
  "answer": "自然语言导购解说，说明选择逻辑与购买建议",
  "recommendations": [
    {{"id": 1, "reason": "推荐理由"}}
  ]
}}
"""


def function_calling_prompt(query: str, history: List[Dict[str, str]] = None) -> str:
    """Function Calling 决策 Prompt：判断是否需要调用工具。"""
    history_text = _history_to_text(history or [])
    return f"""
你是商品销售系统的 AI 助手，负责判断当前用户需求是否需要调用工具获取额外数据。

【对话历史】
{history_text}

【用户需求】{query}

请判断是否需要调用工具：
- 如果用户询问“某商品库存 / 是否有货 / 补货”等，需要调用 query_stock 或 search_products。
- 如果用户询问“卖得最好 / 热销 / 销量排行”，需要调用 get_top_selling。
- 如果用户只是表达一般购物需求，无需调用工具，返回空 tool_calls。
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
