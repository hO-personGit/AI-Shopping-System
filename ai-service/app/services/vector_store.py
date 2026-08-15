import os
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

    def search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        if self.vector_store is None:
            self.load()
        if self.vector_store is None:
            return self._fallback_search(query, top_k)

        docs_and_scores = self.vector_store.similarity_search_with_score(query, k=top_k)
        results = []
        for doc, distance in docs_and_scores:
            item = dict(doc.metadata)
            item["score"] = round(1 / (1 + float(distance)), 4)
            results.append(item)
        return results

    def _fallback_search(self, query: str, top_k: int) -> List[Dict[str, Any]]:
        products = self.repository.fetch_products()
        keywords = [ch for ch in query if not ch.isspace()]
        scored = []
        for product in products:
            text = self._product_to_text(product)
            hit = sum(1 for keyword in keywords if keyword in text)
            sales = int(product.get("salesCount") or 0)
            scored.append((hit * 10 + min(sales, 100), product))
        scored.sort(key=lambda x: x[0], reverse=True)
        return [self._product_to_metadata(product) | {"score": score} for score, product in scored[:top_k]]

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
