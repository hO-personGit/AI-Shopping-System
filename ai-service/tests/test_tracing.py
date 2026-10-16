"""全链路 TraceId 透传单元测试。"""
import logging

from app.tracing import (TRACE_ID_HEADER, TraceIdFilter, get_trace_id,
                         install_log_filter, set_trace_id)


def test_set_and_get_trace_id():
    set_trace_id("abc123")
    assert get_trace_id() == "abc123"
    set_trace_id(None)
    assert get_trace_id() == "-"
    set_trace_id("  ")
    assert get_trace_id() == "-"


def test_trace_id_filter_injects_record_attribute():
    record = logging.LogRecord("test", logging.INFO, "f", 1, "msg", None, None)
    filter_ = TraceIdFilter()
    set_trace_id("trace-xyz")
    assert filter_.filter(record) is True
    assert record.trace_id == "trace-xyz"


def test_install_log_filter_idempotent():
    handler = logging.StreamHandler()
    logging.getLogger().addHandler(handler)
    try:
        install_log_filter()
        install_log_filter()
        count = sum(1 for f in handler.filters if isinstance(f, TraceIdFilter))
        assert count == 1
    finally:
        logging.getLogger().removeHandler(handler)


def test_header_name_constant():
    assert TRACE_ID_HEADER == "X-Trace-Id"
