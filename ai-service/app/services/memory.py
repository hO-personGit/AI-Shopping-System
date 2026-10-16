"""轻量级会话记忆：多轮对话上下文管理。

采用「内存 LRU + TTL」策略，按 session_id 维护最近 N 轮对话历史。
- LRU：会话数量超过上限时淘汰最久未使用的会话。
- TTL：会话超过有效期自动过期，避免内存无限增长。
- 线程安全：基于 threading.Lock 保证并发读写安全。
"""
from __future__ import annotations

import threading
import time
from collections import OrderedDict
from typing import Dict, List


class ConversationMemory:
    def __init__(self, max_sessions: int = 1000, max_turns: int = 6, ttl_seconds: int = 1800):
        self.max_sessions = max_sessions
        self.max_turns = max_turns
        self.ttl_seconds = ttl_seconds
        self._lock = threading.Lock()
        # session_id -> {"turns": [...], "ts": last_access_time}
        self._sessions: "OrderedDict[str, dict]" = OrderedDict()

    def _is_expired(self, item: dict) -> bool:
        return self.ttl_seconds > 0 and (time.time() - item["ts"]) > self.ttl_seconds

    def _touch(self, session_id: str) -> None:
        if session_id in self._sessions:
            self._sessions[session_id]["ts"] = time.time()
            self._sessions.move_to_end(session_id)

    def add_turn(self, session_id: str, user_message: str, assistant_message: str) -> None:
        if not session_id:
            return
        with self._lock:
            if session_id not in self._sessions or self._is_expired(self._sessions[session_id]):
                self._sessions[session_id] = {"turns": [], "ts": time.time()}
            item = self._sessions[session_id]
            item["turns"].append({"role": "user", "content": user_message})
            if assistant_message:
                item["turns"].append({"role": "assistant", "content": assistant_message})
            # 只保留最近 max_turns 轮（user+assistant 两条为一轮）
            keep = self.max_turns * 2
            if len(item["turns"]) > keep:
                item["turns"] = item["turns"][-keep:]
            item["ts"] = time.time()
            self._sessions.move_to_end(session_id)
            self._evict_if_needed()

    def get_history(self, session_id: str) -> List[Dict[str, str]]:
        if not session_id:
            return []
        with self._lock:
            item = self._sessions.get(session_id)
            if item is None or self._is_expired(item):
                return []
            self._touch(session_id)
            return list(item["turns"])

    def clear(self, session_id: str) -> None:
        with self._lock:
            self._sessions.pop(session_id, None)

    def _evict_if_needed(self) -> None:
        while len(self._sessions) > self.max_sessions:
            self._sessions.popitem(last=False)


# 全局单例：FastAPI 进程内共享
memory = ConversationMemory()
