"""测试设计 API — 测试用例生成、脚本生成、数据生成"""
import json
import logging
import re
import secrets
from typing import List, Optional

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, ConfigDict

from app.agents.test_case_agent import test_case_agent
from app.agents.test_script_agent import test_script_agent
from app.agents.test_data_agent import test_data_agent

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/test-design", tags=["测试设计"])

_TRACE_ID_RE = re.compile(r"^[0-9a-f]{16}$")


def _validate_trace_id(x_trace_id: Optional[str], body_trace_id: str) -> str:
    """校验 traceId，返回有效的 traceId"""
    if x_trace_id and x_trace_id == body_trace_id and _TRACE_ID_RE.match(x_trace_id):
        return x_trace_id
    if _TRACE_ID_RE.match(body_trace_id):
        return body_trace_id
    return secrets.token_hex(8)


def _make_sse_event(event: str, data) -> str:
    """构造 SSE 事件；data 为 dict 时 JSON 序列化，为 str 时直接使用"""
    if isinstance(data, dict):
        data_str = json.dumps(data, ensure_ascii=False)
    else:
        data_str = str(data)
    return f"event: {event}\ndata: {data_str}\n\n"


def _error_response(code: int, message: str, trace_id: str) -> dict:
    return {
        "code": code,
        "message": message,
        "data": None,
        "traceId": trace_id,
    }


# ========== 请求模型 ==========

class GenerateTestCaseRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    project_id: int = Field(..., gt=0, description="项目ID")
    requirement: str = Field(..., min_length=1, max_length=10000, description="需求描述")
    doc_ids: Optional[List[int]] = Field(default=None, description="参考文档ID列表")
    context: str = Field(default="", max_length=5000, description="补充上下文")
    trace_id: str = Field(default="", description="追踪ID")


class GenerateTestScriptRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    project_id: int = Field(..., gt=0, description="项目ID")
    test_cases: List[dict] = Field(..., min_length=1, description="测试用例列表")
    module: str = Field(default="", max_length=100, description="模块名称")
    base_url: str = Field(default="", max_length=500, description="被测页面URL")
    trace_id: str = Field(default="", description="追踪ID")


class GenerateTestDataRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    project_id: int = Field(..., gt=0, description="项目ID")
    test_cases: List[dict] = Field(..., min_length=1, description="测试用例列表")
    module: str = Field(default="", max_length=100, description="模块名称")
    trace_id: str = Field(default="", description="追踪ID")


# ========== 接口 ==========

@router.post("/test-cases/generate", summary="生成测试用例（流式）")
async def generate_test_cases(
    req: GenerateTestCaseRequest,
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = _validate_trace_id(x_trace_id, req.trace_id)

    async def stream():
        # meta 帧
        yield _make_sse_event("meta", {
            "traceId": trace_id,
            "type": "test_case",
        })

        full_content = ""
        try:
            async for token in test_case_agent.generate_stream(
                requirement=req.requirement,
                doc_ids=req.doc_ids if req.doc_ids else None,
                context=req.context,
            ):
                full_content += token
                yield _make_sse_event("delta", token)
        except Exception as e:
            logger.error(f"生成测试用例失败: {e}", exc_info=True)
            yield _make_sse_event("error", {"message": f"生成失败: {str(e)}"})
            return

        # 解析最终结果
        from app.agents.test_case_agent import parse_test_cases
        test_cases = parse_test_cases(full_content)

        yield _make_sse_event("done", {
            "finish": True,
            "count": len(test_cases),
            "test_cases": test_cases,
        })

    return StreamingResponse(
        stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Trace-Id": trace_id,
        },
    )


@router.post("/test-scripts/generate", summary="生成测试脚本（流式）")
async def generate_test_scripts(
    req: GenerateTestScriptRequest,
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = _validate_trace_id(x_trace_id, req.trace_id)

    async def stream():
        yield _make_sse_event("meta", {
            "traceId": trace_id,
            "type": "test_script",
        })

        full_content = ""
        try:
            async for token in test_script_agent.generate_stream(
                test_cases=req.test_cases,
                module=req.module,
                base_url=req.base_url,
            ):
                full_content += token
                yield _make_sse_event("delta", token)
        except Exception as e:
            logger.error(f"生成测试脚本失败: {e}", exc_info=True)
            yield _make_sse_event("error", {"message": f"生成失败: {str(e)}"})
            return

        # 提取代码
        code = test_script_agent._extract_code(full_content)

        yield _make_sse_event("done", {
            "finish": True,
            "name": f"{req.module or 'test'}.spec.ts",
            "framework": "playwright",
            "language": "typescript",
            "content": code,
        })

    return StreamingResponse(
        stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Trace-Id": trace_id,
        },
    )


@router.post("/test-data/generate", summary="生成测试数据（流式）")
async def generate_test_data(
    req: GenerateTestDataRequest,
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = _validate_trace_id(x_trace_id, req.trace_id)

    async def stream():
        yield _make_sse_event("meta", {
            "traceId": trace_id,
            "type": "test_data",
        })

        full_content = ""
        try:
            async for token in test_data_agent.generate_stream(
                test_cases=req.test_cases,
                module=req.module,
            ):
                full_content += token
                yield _make_sse_event("delta", token)
        except Exception as e:
            logger.error(f"生成测试数据失败: {e}", exc_info=True)
            yield _make_sse_event("error", {"message": f"生成失败: {str(e)}"})
            return

        # 解析最终结果
        from app.agents.test_data_agent import parse_test_data
        data = parse_test_data(full_content)

        yield _make_sse_event("done", {
            "finish": True,
            "name": data.get("name", "test-data"),
            "data_type": data.get("data_type", "json"),
            "datasets": data.get("datasets", []),
            "count": len(data.get("datasets", [])),
        })

    return StreamingResponse(
        stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Trace-Id": trace_id,
        },
    )
