"""Function Calling 工具集单元测试。"""
from decimal import Decimal


def test_tool_calls_to_text_with_decimal(fake_products):
    """工具结果含 Decimal 等类型时不应序列化失败。"""
    from app.services.tools import tool_calls_to_text
    output = {
        "product_id": 1,
        "name": "有机土豆",
        "price": Decimal("9.90"),
        "stock": 500,
    }
    text = tool_calls_to_text([{"name": "query_stock", "arguments": {}, "output": output}])
    assert "有机土豆" in text
    assert "9.9" in text


def test_tool_executor_query_stock(fake_products):
    from app.services.tools import get_tool_executor
    executor = get_tool_executor()
    result = executor.query_stock(1)
    assert result["product_id"] == 1
    assert result["name"] == "小米手环9 Pro"
    assert "stock" in result


def test_tool_executor_unknown_tool():
    from app.services.tools import get_tool_executor
    executor = get_tool_executor()
    result = executor.execute("not_exist", {})
    assert "error" in result


def test_tool_schemas_well_formed():
    from app.services.tools import TOOL_SCHEMAS
    names = [s["function"]["name"] for s in TOOL_SCHEMAS]
    assert "search_products" in names
    assert "query_stock" in names
    assert "get_top_selling" in names
