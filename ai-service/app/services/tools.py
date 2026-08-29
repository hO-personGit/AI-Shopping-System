"""Function Calling 工具集：AI Agent 可调用的业务工具。

- 每个工具都声明 JSON Schema（name / description / parameters），
  便于大模型通过 Function Calling 决定调用哪个工具、传什么参数。
- ToolExecutor 负责安全执行工具并返回结构化结果。
- 当前内置三个业务工具：商品搜索、库存查询、热销商品查询。
"""
from __future__ import annotations

import json
from typing import Any, Callable, Dict, List

from app.services.db import ProductRepository
from app.services.vector_store import ProductVectorStore

# 工具元数据（给大模型看）
TOOL_SCHEMAS: List[Dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "search_products",
            "description": "根据用户自然语言检索在售商品，返回商品 id、名称、价格、库存、销量等信息",
            "parameters": {
                "type": "object",
                "properties": {"query": {"type": "string", "description": "用户购物需求关键词"}},
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_stock",
            "description": "查询指定商品的实时库存与销售情况，用于回答库存/是否有货类问题",
            "parameters": {
                "type": "object",
                "properties": {"product_id": {"type": "integer", "description": "商品ID"}},
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_top_selling",
            "description": "获取当前销量最高的热销商品列表",
            "parameters": {"type": "object", "properties": {}},
        },
    },
]


class ToolExecutor:
    """工具注册与执行器。"""

    def __init__(self, repository: ProductRepository, vector_store: ProductVectorStore):
        self.repository = repository
        self.vector_store = vector_store
        self._registry: Dict[str, Callable[..., Dict[str, Any]]] = {
            "search_products": self.search_products,
            "query_stock": self.query_stock,
            "get_top_selling": self.get_top_selling,
        }

    def available_tools(self) -> List[str]:
        return list(self._registry.keys())

    def schemas(self) -> List[Dict[str, Any]]:
        return TOOL_SCHEMAS

    def execute(self, name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        func = self._registry.get(name)
        if func is None:
            return {"error": f"未知工具: {name}", "available": self.available_tools()}
        try:
            args = arguments or {}
            return func(**args)
        except TypeError as exc:
            return {"error": f"工具参数错误: {exc}", "tool": name}
        except Exception as exc:
            return {"error": f"工具执行失败: {exc}", "tool": name}

    # ---------- 具体工具 ----------

    def search_products(self, query: str, top_k: int = 5) -> Dict[str, Any]:
        results = self.vector_store.hybrid_search(query, top_k=top_k)
        return {"count": len(results), "products": results}

    def query_stock(self, product_id: int) -> Dict[str, Any]:
        products = self.repository.fetch_products()
        for p in products:
            if int(p.get("id") or -1) == int(product_id):
                return {
                    "product_id": p.get("id"),
                    "name": p.get("name"),
                    "stock": p.get("stock"),
                    "salesCount": p.get("salesCount"),
                    "price": p.get("discountPrice") or p.get("price"),
                }
        return {"error": f"未找到商品 id={product_id}", "product_id": product_id}

    def get_top_selling(self, top_k: int = 5) -> Dict[str, Any]:
        products = self.repository.fetch_products()
        ranked = sorted(products, key=lambda p: int(p.get("salesCount") or 0), reverse=True)
        top = [
            {
                "id": p.get("id"),
                "name": p.get("name"),
                "salesCount": p.get("salesCount"),
                "price": p.get("discountPrice") or p.get("price"),
            }
            for p in ranked[:top_k]
        ]
        return {"count": len(top), "products": top}


# 延迟初始化的全局单例（依赖 vector_store 的全局单例，延迟导入避免循环引用）
_tool_executor: "ToolExecutor | None" = None


def get_tool_executor() -> ToolExecutor:
    global _tool_executor
    if _tool_executor is None:
        from app.services.vector_store import product_vector_store  # 延迟导入
        _tool_executor = ToolExecutor(ProductRepository(), product_vector_store)
    return _tool_executor


def tool_calls_to_text(tool_calls: List[Dict[str, Any]]) -> str:
    """把工具调用结果格式化为可注入 Prompt 的文本。"""
    lines = []
    for call in tool_calls:
        name = call.get("name", "")
        output = call.get("output", {})
        lines.append(f"【工具 {name} 结果】\n{json.dumps(output, ensure_ascii=False)[:800]}")
    return "\n".join(lines)
