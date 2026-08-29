"""pytest 共享夹具与 Mock 数据（不依赖真实 MySQL）。"""
from __future__ import annotations

import os
import sys
from pathlib import Path

# 确保 app 包可导入（ai-service 目录在 sys.path）
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

# 测试默认使用 mock 模式，避免真实调用大模型
os.environ.setdefault("LLM_PROVIDER", "mock")

import pytest  # noqa: E402


@pytest.fixture
def fake_products():
    """与 db_aps.product 表结构一致的模拟商品数据。"""
    return [
        {
            "id": 1, "name": "小米手环9 Pro", "description": "运动健康监测智能手环",
            "price": 249.0, "discountPrice": 229.0, "stock": 120, "salesCount": 1560,
            "placeOfOrigin": "北京", "status": 1, "category": "数码产品",
            "reviewCount": 200, "avgRating": 4.8, "reviewSummary": "续航强，表盘好看",
        },
        {
            "id": 2, "name": "联想蓝牙耳机", "description": "主动降噪无线蓝牙耳机",
            "price": 199.0, "discountPrice": 159.0, "stock": 8, "salesCount": 2310,
            "placeOfOrigin": "深圳", "status": 1, "category": "数码配件",
            "reviewCount": 350, "avgRating": 4.6, "reviewSummary": "降噪效果好",
        },
        {
            "id": 3, "name": "学生双肩背包", "description": "轻便大容量通勤背包",
            "price": 89.0, "discountPrice": 79.0, "stock": 500, "salesCount": 890,
            "placeOfOrigin": "广州", "status": 1, "category": "箱包",
            "reviewCount": 120, "avgRating": 4.5, "reviewSummary": "容量大，背带舒服",
        },
    ]


@pytest.fixture(autouse=True)
def _stub_db(monkeypatch, fake_products):
    """全局替换 DB 访问，所有测试离线运行。"""
    from app.services.db import ProductRepository
    monkeypatch.setattr(ProductRepository, "fetch_products",
                        lambda self: [dict(p) for p in fake_products])
    monkeypatch.setattr(ProductRepository, "log_ai_call",
                        lambda self, *a, **kw: None)
