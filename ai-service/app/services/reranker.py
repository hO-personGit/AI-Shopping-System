"""语义精排（Rerank）：对混合检索结果做二次排序，提升推荐精度。

模式说明（由 RERANK_MODE 配置，默认 lexical，无外部依赖可运行）：
- none   ：不启用精排，直接使用 RRF 融合结果。
- lexical：基于查询词与商品文本的重合度打分（确定性、离线可跑），
           在 RRF 融合基础上做精排，可视为 bge-reranker 的轻量替代。
- api    ：调用 OpenAI 兼容的 Rerank API（如通义千问 rerank / bge-reranker 服务），
           配置 RERANK_BASE_URL / RERANK_MODEL / RERANK_API_KEY；失败自动降级 lexical。
"""
from __future__ import annotations

import logging
import math
from typing import Any, Dict, List, Optional

import requests

from app.config import settings

logger = logging.getLogger("ai-service")


class Reranker:
    def __init__(self, mode: str = "", base_url: str = "", model: str = "",
                 api_key: str = "", top_n: int = 5):
        self.mode = (mode or settings.rerank_mode or "lexical").lower().strip()
        self.base_url = (base_url or settings.rerank_base_url or "").rstrip("/")
        self.model = model or settings.rerank_model or "bge-reranker-v2-m3"
        self.api_key = api_key or settings.rerank_api_key or ""
        self.top_n = top_n if top_n > 0 else settings.rerank_top_n

    def is_enabled(self) -> bool:
        return self.mode != "none"

    def rerank(self, query: str, candidates: List[Dict[str, Any]], top_n: Optional[int] = None) -> List[Dict[str, Any]]:
        """对候选列表精排，返回重排后的 top_n 结果。"""
        if not self.is_enabled() or not candidates:
            return candidates[:top_n or self.top_n]

        if self.mode == "api" and self.api_key and self.base_url:
            try:
                return self._api_rerank(query, candidates, top_n or self.top_n)
            except Exception as exc:  # API 失败降级
                logger.warning("Rerank API 调用失败，降级 lexical：%s", exc)

        return self._lexical_rerank(query, candidates, top_n or self.top_n)

    # ---------- API 模式 ----------

    def _api_rerank(self, query: str, candidates: List[Dict[str, Any]], top_n: int) -> List[Dict[str, Any]]:
        url = self.base_url + "/rerank"
        docs = [self._text_of(c) for c in candidates]
        resp = requests.post(
            url,
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={"model": self.model, "query": query, "documents": docs, "top_n": min(top_n, len(candidates))},
            timeout=settings.llm_timeout,
        )
        resp.raise_for_status()
        data = resp.json()
        results = data.get("results") or []
        reordered = []
        for item in results:
            idx = item.get("index", 0)
            score = item.get("relevance_score", 0) or item.get("score", 0)
            if 0 <= idx < len(candidates):
                cand = dict(candidates[idx])
                cand["rerankScore"] = round(float(score), 4)
                cand["rerankSource"] = "api"
                reordered.append(cand)
        return reordered[:top_n]

    # ---------- 词法模式（确定性精排，无外部依赖） ----------

    def _lexical_rerank(self, query: str, candidates: List[Dict[str, Any]], top_n: int) -> List[Dict[str, Any]]:
        tokens = self._tokenize(query)
        if not tokens:
            return candidates[:top_n]
        scored = []
        for cand in candidates:
            text = self._text_of(cand).lower()
            hit = sum(1 for t in tokens if t in text)
            name = str(cand.get("name") or "").lower()
            name_hit = sum(1 for t in tokens if t in name)
            # 词法命中 + 名称命中加权 + 原 RRF 分数小幅保持
            score = hit * 1.0 + name_hit * 1.5 + float(cand.get("score") or 0) * 0.1
            item = dict(cand)
            item["rerankScore"] = round(score, 4)
            item["rerankSource"] = "lexical"
            scored.append(item)
        scored.sort(key=lambda x: x["rerankScore"], reverse=True)
        return scored[:top_n]

    # ---------- 工具方法 ----------

    def _text_of(self, cand: Dict[str, Any]) -> str:
        parts = [cand.get("name", ""), cand.get("category", ""), cand.get("description", ""),
                 cand.get("placeOfOrigin", "")]
        return " ".join(str(p) for p in parts if p)

    def _tokenize(self, text: str) -> List[str]:
        text = (text or "").lower()
        words = [w for w in text.replace("，", " ").replace("。", " ").replace("？", " ").split() if w]
        chars = [ch for ch in text if not ch.isspace() and not ch.isascii()]
        return list(dict.fromkeys(words + chars))


# 单例：由 vector_store 在混合检索后调用
reranker = Reranker()
