import json
import re
from typing import Any, Dict

import requests

from app.config import settings


class LLMClient:
    def __init__(self):
        self.provider = settings.llm_provider.lower().strip()

    def generate_text(self, prompt: str) -> str:
        if self.provider == "mock" or not settings.llm_api_key:
            return ""

        url = self._chat_url()
        headers = {
            "Authorization": f"Bearer {settings.llm_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": settings.llm_model,
            "messages": [
                {"role": "system", "content": "你是商品销售系统中的专业中文 AI 助手，只输出业务可用内容。"},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.4,
        }
        response = requests.post(url, headers=headers, json=payload, timeout=settings.llm_timeout)
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

    def _chat_url(self) -> str:
        if settings.llm_base_url:
            return settings.llm_base_url.rstrip("/") + "/chat/completions"
        if self.provider in {"dashscope", "qwen", "tongyi"}:
            return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        if self.provider == "zhipu":
            return "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        return "https://api.openai.com/v1/chat/completions"

    def _extract_json(self, text: str) -> str:
        cleaned = re.sub(r"```json|```", "", text).strip()
        start = cleaned.find("{")
        end = cleaned.rfind("}")
        if start >= 0 and end >= start:
            return cleaned[start:end + 1]
        return cleaned


llm_client = LLMClient()
