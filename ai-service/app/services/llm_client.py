"""大模型客户端：统一封装文本生成、JSON 生成与流式生成。

支持多 Provider（mock / openai / dashscope / zhipu / openai_compatible），
通过 .env 的 LLM_PROVIDER 切换，兼容 OpenAI Chat Completions 协议。
"""
from __future__ import annotations

import json
import re
from typing import Any, Dict, Generator, List, Optional

import requests

from app.config import settings


class LLMClient:
    def __init__(self):
        self.provider = settings.llm_provider.lower().strip()

    # ---------- 基础能力 ----------

    def _chat_url(self) -> str:
        if settings.llm_base_url:
            return settings.llm_base_url.rstrip("/") + "/chat/completions"
        if self.provider in {"dashscope", "qwen", "tongyi"}:
            return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        if self.provider == "zhipu":
            return "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        return "https://api.openai.com/v1/chat/completions"

    def _headers(self) -> Dict[str, str]:
        return {
            "Authorization": f"Bearer {settings.llm_api_key}",
            "Content-Type": "application/json",
        }

    def _messages(self, prompt: str, system: str = "") -> List[Dict[str, str]]:
        return [
            {"role": "system", "content": system or "你是商品销售系统中的专业中文 AI 助手，只输出业务可用内容。"},
            {"role": "user", "content": prompt},
        ]

    def _payload(self, messages: List[Dict[str, str]], temperature: float = 0.4,
                 tools: Optional[List[Dict[str, Any]]] = None, stream: bool = False) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "model": settings.llm_model,
            "messages": messages,
            "temperature": temperature,
            "stream": stream,
        }
        if tools:
            payload["tools"] = tools
        return payload

    # ---------- 文本生成 ----------

    def generate_text(self, prompt: str, system: str = "") -> str:
        if self.provider == "mock" or not settings.llm_api_key:
            return ""
        url = self._chat_url()
        response = requests.post(url, headers=self._headers(),
                                 json=self._payload(self._messages(prompt, system)),
                                 timeout=settings.llm_timeout)
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]

    def generate_json(self, prompt: str, fallback: Dict[str, Any]) -> Dict[str, Any]:
        text = self.generate_text(prompt)
        if not text:
            return fallback
        try:
            return json.loads(self._extract_json(text))
        except Exception:
            result = dict(fallback)
            result["rawText"] = text
            return result

    # ---------- 流式生成（SSE） ----------

    def generate_stream(self, prompt: str, system: str = "") -> Generator[str, None, None]:
        """逐段产出文本（用于 SSE 打字机效果）。"""
        if self.provider == "mock" or not settings.llm_api_key:
            # mock 模式：无真实模型，直接返回占位文案，按句切分模拟流式
            placeholder = "已为你整理推荐建议（当前为演示模式，接入大模型 API 后输出更精准）。"
            for chunk in self._chunk_text(placeholder):
                yield chunk
            return

        url = self._chat_url()
        with requests.post(url, headers=self._headers(),
                           json=self._payload(self._messages(prompt, system), stream=True),
                           timeout=settings.llm_timeout, stream=True) as response:
            response.raise_for_status()
            for line in response.iter_lines(decode_unicode=True):
                if not line:
                    continue
                line = line.strip()
                if line.startswith("data:"):
                    data = line[len("data:"):].strip()
                else:
                    continue
                if data == "[DONE]":
                    break
                try:
                    payload = json.loads(data)
                    delta = payload["choices"][0]["delta"].get("content") or ""
                    if delta:
                        yield delta
                except Exception:
                    continue

    @staticmethod
    def _chunk_text(text: str, size: int = 24) -> Generator[str, None, None]:
        for i in range(0, len(text), size):
            yield text[i:i + size]

    # ---------- Function Calling ----------

    def call_with_tools(self, prompt: str, tools: List[Dict[str, Any]],
                        system: str = "") -> Dict[str, Any]:
        """调用大模型并返回 tool_calls（若模型要求调用工具）。

        返回结构：{"tool_calls": [{"name":..., "arguments": {...}}], "text": ...}
        mock 模式下不触发真实工具调用，返回空 tool_calls。
        """
        if self.provider == "mock" or not settings.llm_api_key:
            return {"tool_calls": [], "text": ""}
        url = self._chat_url()
        response = requests.post(url, headers=self._headers(),
                                 json=self._payload(self._messages(prompt, system), tools=tools),
                                 timeout=settings.llm_timeout)
        response.raise_for_status()
        data = response.json()
        message = data["choices"][0]["message"]
        tool_calls = []
        for tc in message.get("tool_calls") or []:
            fn = tc.get("function", {})
            args = fn.get("arguments") or "{}"
            try:
                parsed_args = json.loads(args) if isinstance(args, str) else args
            except Exception:
                parsed_args = {}
            tool_calls.append({"name": fn.get("name"), "arguments": parsed_args})
        return {"tool_calls": tool_calls, "text": message.get("content") or ""}

    # ---------- 工具方法 ----------

    def _extract_json(self, text: str) -> str:
        cleaned = re.sub(r"```json|```", "", text).strip()
        start = cleaned.find("{")
        end = cleaned.rfind("}")
        if start >= 0 and end >= start:
            return cleaned[start:end + 1]
        return cleaned


llm_client = LLMClient()
