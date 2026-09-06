"""Milvus 向量库适配层（可插拔：FAISS ↔ Milvus）。

- pymilvus 为可选依赖：未安装 / 连接失败 / 集合不存在时抛出 MilvusUnavailableError，
  由上层 ProductVectorStore 捕获并自动降级 FAISS，保证检索链路不中断。
- 集合结构：id(int64 主键) + vector(float_vector, dim) + metadata(json)。
- 检索返回与 FAISS 同构的 item dict（含 id/name/category/.../score），上层无需感知后端差异。
"""
from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

from app.config import settings

logger = logging.getLogger("ai-service")


class MilvusUnavailableError(RuntimeError):
    """Milvus 不可用（未安装客户端 / 连接失败 / 集合异常）。"""


def _import_pymilvus():
    try:
        from pymilvus import MilvusClient  # noqa: F401
        return MilvusClient
    except ImportError as exc:  # pragma: no cover - 依赖缺失路径
        raise MilvusUnavailableError("pymilvus 未安装，Milvus 向量库不可用") from exc


class MilvusStore:
    """商品向量 Milvus 存储：建集、写入、检索。"""

    def __init__(self, embedding_dim: int = 384):
        self.embedding_dim = embedding_dim
        self.uri = (settings.milvus_uri or f"http://{settings.milvus_host}:{settings.milvus_port}")
        self.collection = settings.milvus_collection
        self._client = None
        self._connect()

    def _connect(self) -> None:
        MilvusClient = _import_pymilvus()
        try:
            self._client = MilvusClient(uri=self.uri, timeout=settings.milvus_timeout)
            # 探活：集合不存在则创建
            if not self._client.has_collection(self.collection):
                self._create_collection()
            logger.info("Milvus 已连接 uri=%s collection=%s", self.uri, self.collection)
        except MilvusUnavailableError:
            raise
        except Exception as exc:
            raise MilvusUnavailableError(f"Milvus 连接失败: {exc}") from exc

    def _create_collection(self) -> None:
        schema = {
            "field_name": "id",
            "data_type": "INT64",
            "is_primary": True,
            "auto_id": False,
        }
        # pymilvus 2.4+ 的 create_collection 简写
        self._client.create_collection(
            collection_name=self.collection,
            dimension=self.embedding_dim,
            primary_field_name="id",
            vector_field_name="vector",
            metric_type="COSINE",
            auto_id=False,
        )
        logger.info("Milvus 集合已创建 collection=%s dim=%s", self.collection, self.embedding_dim)

    def upsert(self, items: List[Dict[str, Any]]) -> int:
        """写入/更新向量数据。items 每项含 id + vector + metadata 字段。"""
        if self._client is None:
            raise MilvusUnavailableError("Milvus 未连接")
        rows = []
        for it in items:
            row = {
                "id": int(it["id"]),
                "vector": it["vector"],
            }
            metadata = {k: v for k, v in it.items() if k not in ("id", "vector")}
            row["metadata"] = metadata
            rows.append(row)
        self._client.insert(self.collection, rows)
        return len(rows)

    def search(self, vector: List[float], top_k: int = 5) -> List[Dict[str, Any]]:
        """按向量检索，返回与 FAISS 同构的结果列表。"""
        if self._client is None:
            raise MilvusUnavailableError("Milvus 未连接")
        res = self._client.search(
            collection_name=self.collection,
            data=[vector],
            limit=top_k,
            output_fields=["metadata"],
        )
        results: List[Dict[str, Any]] = []
        if not res:
            return results
        for hit in res[0]:
            entity = hit.get("entity") or {}
            metadata = entity.get("metadata") or {}
            item = dict(metadata)
            item["score"] = round(float(hit.get("distance") or 0), 4)
            results.append(item)
        return results

    def count(self) -> int:
        if self._client is None:
            return 0
        return self._client.get_collection_stats(self.collection).get("row_count", 0)

    def drop(self) -> None:
        if self._client is not None:
            self._client.drop_collection(self.collection)
