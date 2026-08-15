import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT))

from app.services.vector_store import product_vector_store


if __name__ == "__main__":
    count = product_vector_store.build(force=True)
    print(f"商品向量库初始化完成，写入 {count} 个商品。")
