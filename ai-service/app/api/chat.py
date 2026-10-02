import json
import logging
import re
import secrets
from typing import Optional

from fastapi import APIRouter, Header, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, field_validator, ConfigDict

from app.graph.nodes import stream_echo

logger = logging.getLogger(__name__)

router = APIRouter(tags=["聊天"])

# 16 位小写十六进制正则
_TRACE_ID_RE = re.compile(r"^[0-9a-f]{16}$")


class ChatStreamRequest(BaseModel):
    """流式聊天请求体"""
    model_config = ConfigDict(extra="forbid")  # 拒绝未知字段

    session_id: str = Field(..., description="会话ID，UUID格式")
    message: str = Field(..., min_length=1, max_length=2000, description="用户消息")
    trace_id: str = Field(..., description="追踪ID，16位小写十六进制")
    user_id: int = Field(..., gt=0, description="用户ID")

    @field_validator("trace_id")
    @classmethod
    def validate_trace_id(cls, v: str) -> str:
        if not _TRACE_ID_RE.match(v):
            raise ValueError("traceId 格式错误，应为16位小写十六进制")
        return v

    @field_validator("session_id")
    @classmethod
    def validate_session_id(cls, v: str) -> str:
        # UUID 格式简单校验
        if len(v) < 8 or len(v) > 64:
            raise ValueError("sessionId 格式错误")
        return v


def _make_sse_event(event: str, data: dict) -> str:
    """构造一个 SSE 事件"""
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


def _error_response(code: int, message: str, trace_id: str):
    """构造统一格式的错误响应"""
    return {
        "code": code,
        "message": message,
        "data": None,
        "traceId": trace_id
    }


async def _stream_chat(request: ChatStreamRequest):
    """生成 SSE 流式响应"""
    trace_id = request.trace_id

    # 第一帧：meta
    yield _make_sse_event("meta", {
        "traceId": trace_id,
        "sessionId": request.session_id
    })

    # 中间帧：delta，逐字输出
    async for token in stream_echo(request.message):
        yield _make_sse_event("delta", {"content": token})

    # 最后一帧：done
    yield _make_sse_event("done", {"finish": True})


@router.post("/chat/stream", summary="流式聊天（占位）")
async def chat_stream(
    request: ChatStreamRequest,
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    # 校验：请求头的 traceId 必须与 body 的 traceId 一致
    if x_trace_id is None or x_trace_id != request.trace_id:
        raise HTTPException(
            status_code=400,
            detail=_error_response(1002, "追踪头不一致", request.trace_id)
        )

    return StreamingResponse(
        _stream_chat(request),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Trace-Id": request.trace_id,
        }
    )
