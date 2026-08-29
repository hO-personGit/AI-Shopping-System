from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """AI service runtime configuration."""

    app_name: str = "AI商品销售系统服务"
    host: str = "0.0.0.0"
    port: int = 8001

    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: str = "root"
    mysql_database: str = "db_aps"
    mysql_charset: str = "utf8mb4"

    faiss_index_path: str = "data/product_faiss"
    embedding_dim: int = 384

    llm_provider: str = "mock"
    llm_api_key: str = ""
    llm_base_url: str = ""
    llm_model: str = "qwen-plus"
    llm_timeout: int = 40

    # 检索：hybrid（向量+关键词多路召回）/ vector / keyword
    retrieval_mode: str = "hybrid"
    retrieval_top_k: int = 5
    hybrid_vector_weight: float = 0.6
    hybrid_keyword_weight: float = 0.4

    # 精排（Rerank）：none / lexical（默认，无依赖） / api（接 Rerank 服务）
    rerank_mode: str = "lexical"
    rerank_model: str = "bge-reranker-v2-m3"
    rerank_api_key: str = ""
    rerank_base_url: str = ""
    rerank_top_n: int = 5

    # 缓存：问答结果 TTL（秒）
    cache_ttl_seconds: int = 600
    cache_max_size: int = 256

    # 多轮对话记忆
    memory_max_turns: int = 6
    memory_ttl_seconds: int = 1800
    memory_max_sessions: int = 1000

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @property
    def index_path(self) -> Path:
        return Path(self.faiss_index_path)


settings = Settings()
