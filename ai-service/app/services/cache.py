"""问答结果缓存：降低大模型调用成本、提升响应速度。

策略：以「规范化后的 query + top_k」为 key，缓存最近 N 条问答结果，TTL 过期自动失效。
命中缓存时直接返回，避免重复调用大模型与向量检索。
"""
from __future__ import annotations

import hashlib
import re
import threading
import time
from collections import OrderedDict
from typing import Any, Optional


class TTLCache:
    def __init__(self, max_size: int = 256, ttl_seconds: int = 600):
        self.max_size = max_size
        self.ttl_seconds = ttl_seconds
        self._lock = threading.Lock()
        # key -> {"value": ..., "ts": ...}
        self._data: "OrderedDict[str, dict]" = OrderedDict()

    @staticmethod
    def normalize(text: str) -> str:
        """规范化 key：去空白、统一小写，降低同义问题缓存差异。"""
        text = re.sub(r"\s+", "", text or "").lower()
        return text

    @staticmethod
    def make_key(query: str, top_k: int, scope: str = "guide") -> str:
        raw = f"{scope}:{top_k}:{TTLCache.normalize(query)}"
        digest = hashlib.md5(raw.encode("utf-8")).hexdigest()
        return f"{scope}:{digest}"

    def get(self, key: str) -> Optional[Any]:
        with self._lock:
            item = self._data.get(key)
            if item is None:
                return None
            if self.ttl_seconds > 0 and (time.time() - item["ts"]) > self.ttl_seconds:
                self._data.pop(key, None)
                return None
            self._data.move_to_end(key)
            return item["value"]

    def set(self, key: str, value: Any) -> None:
        with self._lock:
            self._data[key] = {"value": value, "ts": time.time()}
            self._data.move_to_end(key)
            while len(self._data) > self.max_size:
                self._data.popitem(last=False)

    def clear(self) -> None:
        with self._lock:
            self._data.clear()

    def size(self) -> int:
        with self._lock:
            return len(self._data)


# 全局单例
answer_cache = TTLCache()
