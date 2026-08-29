"""问答结果缓存单元测试。"""
import time


def test_set_and_get():
    from app.services.cache import TTLCache
    c = TTLCache(max_size=10, ttl_seconds=60)
    key = c.make_key("推荐性价比手机", 5)
    value = {"answer": "已推荐"}
    c.set(key, value)
    assert c.get(key) == value


def test_cache_miss():
    from app.services.cache import TTLCache
    c = TTLCache()
    assert c.get("not-exist") is None


def test_ttl_expire():
    import time
    from app.services.cache import TTLCache
    c = TTLCache(max_size=10, ttl_seconds=60)
    key = c.make_key("query", 5)
    c.set(key, "v")
    # 直接篡改写入时间，模拟过期
    c._data[key]["ts"] = time.time() - 9999
    assert c.get(key) is None


def test_normalize_key_same():
    from app.services.cache import TTLCache
    k1 = TTLCache.make_key(" 推荐 手机 ", 5)
    k2 = TTLCache.make_key("推荐手机", 5)
    assert k1 == k2


def test_max_size_eviction():
    from app.services.cache import TTLCache
    c = TTLCache(max_size=2, ttl_seconds=60)
    c.set("k1", 1)
    c.set("k2", 2)
    c.set("k3", 3)
    assert c.get("k1") is None
    assert c.get("k2") == 2
    assert c.get("k3") == 3
