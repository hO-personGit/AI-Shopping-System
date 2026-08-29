"""商品向量检索与混合检索（多路召回 + 重排）。

- 向量检索：基于 FAISS 的语义相似度召回。
- 关键词检索：基于词频加权的 BM25-like 召回（不依赖外部服务）。
- 混合检索：向量 + 关键词多路召回，使用 RRF（Reciprocal Rank Fusion）融合，
  再结合销量权重进行重排，提升推荐精度与稳定性。
"""
from __future__ import annotations

import math
import os
import threading
import time
from pathlib import Path
from typing import Any, Dict, List

from langchain_community.vectorstores import FAISS

from app.config import settings
from app.services.db import ProductRepository
from app.services.embeddings import HashEmbeddings


class ProductVectorStore:
    def __init__(self):
        self.embedding = HashEmbeddings(settings.embedding_dim)
        self.repository = ProductRepository()
        self.vector_store = None
        self._lock = threading.Lock()

    # ---------- 索引构建 / 加载 ----------

    def build(self, force: bool = False) -> int:
        index_path = Path(settings.faiss_index_path)
        if index_path.exists() and not force:
            self.load()
            return 0

        products = self.repository.fetch_products()
        texts = [self._product_to_text(product) for product in products]
        metadatas = [self._product_to_metadata(product) for product in products]
        if not texts:
            self.vector_store = None
            return 0

        index_path.parent.mkdir(parents=True, exist_ok=True)
        self.vector_store = FAISS.from_texts(texts=texts, embedding=self.embedding, metadatas=metadatas)
        self.vector_store.save_local(str(index_path))
        return len(texts)

    def load(self):
        index_path = Path(settings.faiss_index_path)
        if index_path.exists() and (index_path / "index.faiss").exists():
            self.vector_store = FAISS.load_local(
                str(index_path),
                self.embedding,
                allow_dangerous_deserialization=True,
            )
        else:
            self.build(force=True)
        return self.vector_store

    # ---------- 单路召回 ----------

    def search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        """FAISS 语义向量检索。"""
        with self._lock:
            if self.vector_store is None:
                self.load()
        if self.vector_store is None:
            return self.keyword_search(query, top_k)

        docs_and_scores = self.vector_store.similarity_search_with_score(query, k=top_k)
        results = []
        for doc, distance in docs_and_scores:
            item = dict(doc.metadata)
            item["score"] = round(1 / (1 + float(distance)), 4)
            results.append(item)
        return results

    def keyword_search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        """BM25-like 关键词检索：对商品文本按词频 + 字段权重打分。"""
        products = self.repository.fetch_products()
        tokens = self._tokenize(query)
        if not tokens:
            return []
        scored = []
        for product in products:
            text = self._product_to_text(product).lower()
            score = 0.0
            for token in tokens:
                count = text.count(token)
                if count:
                    # 简单 tf 加权，字段命中（名称/分类）额外加分
                    score += count * (1.0 + math.log1p(count))
            name = str(product.get("name") or "").lower()
            category = str(product.get("category") or "").lower()
            if any(t in name for t in tokens):
                score *= 1.6
            if any(t in category for t in tokens):
                score *= 1.3
            if score > 0:
                sales = int(product.get("salesCount") or 0)
                # 加入销量小权重，保证同分时热销商品更靠前
                score += math.log1p(sales) * 0.05
                item = self._product_to_metadata(product)
                item["score"] = round(score, 4)
                scored.append(item)
        scored.sort(key=lambda x: x["score"], reverse=True)
        return scored[:top_k]

    # ---------- 混合检索（多路召回 + RRF 融合 + 重排） ----------

    def hybrid_search(self, query: str, top_k: int = 5,
                      vector_weight: float = 0.6, keyword_weight: float = 0.4,
                      sales_boost: bool = True) -> List[Dict[str, Any]]:
        """向量 + 关键词多路召回，RRF 融合后按加权分重排。

        RRF 公式：score = Σ 1 / (k + rank)，k 取 60。
        融合后再叠加销量归一化权重，得到最终推荐排序。
        """
        if top_k <= 0:
            return []
        recall_k = max(top_k * 2, 10)
        vector_hits = self.search(query, recall_k)
        keyword_hits = self.keyword_search(query, recall_k)

        # 多路召回结果按 id 合并，记录各自排名
        rank_map: Dict[str, Dict[str, Any]] = {}
        for idx, hit in enumerate(vector_hits):
            pid = str(hit.get("id"))
            entry = rank_map.setdefault(pid, dict(hit))
            entry["_vector_rank"] = idx + 1
            entry.setdefault("_vector_score", hit.get("score", 0))
            entry.setdefault("_keyword_rank", recall_k + 1)
            entry.setdefault("_keyword_score", 0)
        for idx, hit in enumerate(keyword_hits):
            pid = str(hit.get("id"))
            entry = rank_map.setdefault(pid, dict(hit))
            entry["_keyword_rank"] = idx + 1
            entry["_keyword_score"] = hit.get("score", 0)
            entry.setdefault("_vector_rank", recall_k + 1)
            entry.setdefault("_vector_score", 0)

        k = 60
        merged = []
        for pid, entry in rank_map.items():
            rrf = 1.0 / (k + entry["_vector_rank"]) + 1.0 / (k + entry["_keyword_rank"])
            score = vector_weight * entry["_vector_score"] + keyword_weight * entry["_keyword_score"]
            if sales_boost:
                # 销量归一化加成（0~1），体现“卖得好”的运营信号
                score += math.log1p(entry.get("salesCount") or 0) * 0.05
            entry["score"] = round(score, 4)
            entry["rrfScore"] = round(rrf, 4)
            merged.append(entry)

        merged.sort(key=lambda x: x["score"], reverse=True)
        # 清理内部字段，返回干净结果
        for entry in merged:
            for f in ("_vector_rank", "_keyword_rank", "_vector_score", "_keyword_score"):
                entry.pop(f, None)
        return merged[:top_k]

    def _fallback_search(self, query: str, top_k: int) -> List[Dict[str, Any]]:
        # 兼容旧引用：退化为关键词检索
        return self.keyword_search(query, top_k)

    # ---------- 文本 / 元数据 ----------

    def _tokenize(self, text: str) -> List[str]:
        text = (text or "").lower()
        chars = [ch for ch in text if not ch.isspace() and not ch.isascii()]
        words = [w for w in text.replace("，", " ").replace("。", " ").split() if w]
        bigrams = [text[i:i + 2] for i in range(max(0, len(text) - 1)) if not text[i:i + 2].isspace()]
        return list(dict.fromkeys(chars + words + bigrams))

    def _product_to_text(self, product: Dict[str, Any]) -> str:
        return "\n".join([
            f"商品ID：{product.get('id')}",
            f"名称：{product.get('name', '')}",
            f"分类：{product.get('category', '')}",
            f"价格：{product.get('price', '')}",
            f"优惠价：{product.get('discountPrice', '')}",
            f"库存：{product.get('stock', '')}",
            f"销量：{product.get('salesCount', '')}",
            f"产地：{product.get('placeOfOrigin', '')}",
            f"评价均分：{product.get('avgRating', '')}",
            f"评价摘要：{product.get('reviewSummary', '')}",
            f"描述：{product.get('description', '')}",
        ])

    def _product_to_metadata(self, product: Dict[str, Any]) -> Dict[str, Any]:
        price = product.get("discountPrice") or product.get("price") or 0
        return {
            "id": product.get("id"),
            "name": product.get("name") or "",
            "category": product.get("category") or "",
            "price": float(price),
            "stock": int(product.get("stock") or 0),
            "salesCount": int(product.get("salesCount") or 0),
            "description": product.get("description") or "",
            "placeOfOrigin": product.get("placeOfOrigin") or "",
            "avgRating": float(product.get("avgRating") or 0),
        }


product_vector_store = ProductVectorStore()
