"""FastAPI 接口测试：健康检查、导购接口、SSE 流式接口（mock 离线模式）。"""
import json

from fastapi.testclient import TestClient


def test_health():
    from app.main import app
    with TestClient(app) as client:
        resp = client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "ok"
        assert data["version"] == "2.0.0"


def test_guide_endpoint(monkeypatch, fake_products):
    from app.main import app
    from app.services.ai_service import product_ai_service
    from app.services.vector_store import ProductVectorStore

    store = ProductVectorStore()
    monkeypatch.setattr(store, "hybrid_search",
                        lambda q, k, **kw: [store._product_to_metadata(p) | {"score": 0.9} for p in fake_products[:2]])
    monkeypatch.setattr("app.services.ai_service.product_vector_store", store)

    with TestClient(app) as client:
        resp = client.post("/ai/guide", json={"query": "推荐蓝牙耳机", "userId": 7, "topK": 3})
        assert resp.status_code == 200
        data = resp.json()
        assert data["answer"]
        assert len(data["recommendations"]) > 0
        assert data["source"] == "mock"


def test_stream_guide_endpoint(monkeypatch, fake_products):
    from app.main import app
    from app.services.vector_store import ProductVectorStore

    store = ProductVectorStore()
    monkeypatch.setattr(store, "hybrid_search",
                        lambda q, k, **kw: [store._product_to_metadata(p) | {"score": 0.9} for p in fake_products[:2]])
    monkeypatch.setattr("app.services.ai_service.product_vector_store", store)

    with TestClient(app) as client:
        with client.stream("POST", "/ai/guide/stream", json={"query": "推荐耳机"}) as resp:
            assert resp.status_code == 200
            assert resp.headers["content-type"].startswith("text/event-stream")
            body = "".join(resp.iter_text())
            assert "data:" in body
            assert "delta" in body
            assert "[DONE]" in body


def test_copywriting_endpoint(monkeypatch):
    from app.main import app
    with TestClient(app) as client:
        resp = client.post("/ai/copywriting", json={"name": "蓝牙耳机", "category": "数码配件", "price": 199})
        assert resp.status_code == 200
        assert resp.json()["title"]


def test_clear_session(monkeypatch):
    from app.main import app
    from app.services.memory import memory
    memory.add_turn("sess-clear", "q", "a")
    with TestClient(app) as client:
        resp = client.post("/ai/chat/clear", params={"session_id": "sess-clear"})
        assert resp.status_code == 200
        assert resp.json()["cleared"] is True
        assert memory.get_history("sess-clear") == []
