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

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @property
    def index_path(self) -> Path:
        return Path(self.faiss_index_path)


settings = Settings()
