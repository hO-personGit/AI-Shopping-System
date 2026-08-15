import hashlib
from typing import List

import numpy as np
from langchain_core.embeddings import Embeddings


class HashEmbeddings(Embeddings):
    """Deterministic local embedding for no-key demos.

    It keeps the project runnable without paid embedding APIs. The vectors are
    not as accurate as production embeddings, but they are enough for portfolio
    demonstration and can be replaced later with OpenAI/Qwen embeddings.
    """

    def __init__(self, dim: int = 384):
        self.dim = dim

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> List[float]:
        return self._embed(text)

    def _embed(self, text: str) -> List[float]:
        vector = np.zeros(self.dim, dtype="float32")
        normalized = (text or "").lower()
        tokens = self._tokens(normalized)
        for token in tokens:
            digest = hashlib.md5(token.encode("utf-8")).digest()
            idx = int.from_bytes(digest[:4], "big") % self.dim
            sign = 1 if digest[4] % 2 == 0 else -1
            vector[idx] += sign
        norm = np.linalg.norm(vector)
        if norm > 0:
            vector = vector / norm
        return vector.tolist()

    def _tokens(self, text: str) -> List[str]:
        words = [w for w in text.replace("，", " ").replace("。", " ").split() if w]
        chars = [ch for ch in text if not ch.isspace()]
        bigrams = [text[i:i + 2] for i in range(max(0, len(text) - 1)) if not text[i:i + 2].isspace()]
        return words + chars + bigrams
