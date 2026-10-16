"""全链路 TraceId 透传（与 Java 后端 X-Trace-Id 约定对齐）。

- 中间件从请求头 X-Trace-Id 读取上游 TraceId，无则生成，写入 contextvar；
- logging.Filter 把 trace_id 注入每条日志记录，格式统一输出；
- 后续调外部服务（LLM / Rerank）时可继续透传该值。
"""
from __future__ import annotations

import contextvars
import logging

TRACE_ID_HEADER = "X-Trace-Id"

_trace_id_var: contextvars.ContextVar[str] = contextvars.ContextVar("trace_id", default="-")


class TraceIdFilter(logging.Filter):
    """把当前上下文中的 trace_id 注入日志记录。"""

    def filter(self, record: logging.LogRecord) -> bool:
        record.trace_id = _trace_id_var.get()
        return True


def set_trace_id(trace_id: str) -> None:
    _trace_id_var.set((trace_id or "-").strip() or "-")


def get_trace_id() -> str:
    return _trace_id_var.get()


def install_log_filter() -> None:
    """为根 logger 的所有 handler 安装 TraceIdFilter，使日志统一输出 [trace_id]。"""
    for handler in logging.getLogger().handlers:
        if not any(isinstance(f, TraceIdFilter) for f in handler.filters):
            handler.addFilter(TraceIdFilter())
