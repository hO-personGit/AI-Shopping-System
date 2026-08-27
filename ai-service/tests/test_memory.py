"""会话记忆（多轮对话上下文）单元测试。"""
import time


def test_add_and_get_history():
    from app.services.memory import ConversationMemory
    m = ConversationMemory(max_sessions=10, max_turns=2, ttl_seconds=60)
    m.add_turn("s1", "你好", "你好，有什么可以帮你")
    m.add_turn("s1", "推荐手机", "好的，推荐……")
    history = m.get_history("s1")
    assert len(history) == 4
    assert history[0]["role"] == "user"
    assert history[-1]["content"] == "好的，推荐……"


def test_max_turns_trim():
    from app.services.memory import ConversationMemory
    m = ConversationMemory(max_sessions=10, max_turns=1, ttl_seconds=60)
    for i in range(3):
        m.add_turn("s1", f"q{i}", f"a{i}")
    history = m.get_history("s1")
    # 只保留最近 1 轮（2 条）
    assert len(history) == 2
    assert history[0]["content"] == "q2"


def test_ttl_expire():
    import time
    from app.services.memory import ConversationMemory
    m = ConversationMemory(max_sessions=10, max_turns=5, ttl_seconds=60)
    m.add_turn("s1", "q", "a")
    # 直接篡改最后访问时间，模拟过期
    m._sessions["s1"]["ts"] = time.time() - 9999
    assert m.get_history("s1") == []


def test_clear():
    from app.services.memory import ConversationMemory
    m = ConversationMemory()
    m.add_turn("s1", "q", "a")
    m.clear("s1")
    assert m.get_history("s1") == []


def test_lru_eviction():
    from app.services.memory import ConversationMemory
    m = ConversationMemory(max_sessions=2, max_turns=5, ttl_seconds=60)
    m.add_turn("s1", "q", "a")
    m.add_turn("s2", "q", "a")
    m.add_turn("s3", "q", "a")  # 触发淘汰
    assert m.get_history("s1") == []
    assert len(m.get_history("s2")) == 2
    assert len(m.get_history("s3")) == 2
