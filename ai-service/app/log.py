import logging
import sys

from app.config import settings


def setup_logging():
    level = logging.DEBUG if settings.app_env == "development" else logging.INFO
    fmt = "%(asctime)s [%(levelname)s] [%(name)s] [%(trace_id)s] %(message)s"

    formatter = _TraceFormatter(fmt)
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)

    root = logging.getLogger()
    root.setLevel(level)
    root.handlers.clear()
    root.addHandler(handler)


class _TraceFormatter(logging.Formatter):
    """带 trace_id 占位的格式化器，缺省显示 '-'"""

    def format(self, record):
        if not hasattr(record, "trace_id"):
            record.trace_id = "-"
        return super().format(record)


class TraceLogFilter(logging.Filter):
    """给日志记录注入 trace_id"""

    def __init__(self, trace_id: str):
        super().__init__()
        self.trace_id = trace_id

    def filter(self, record):
        record.trace_id = self.trace_id
        return True
