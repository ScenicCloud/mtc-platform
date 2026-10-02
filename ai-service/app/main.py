import json
import secrets

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.health import router as health_router
from app.api.chat import router as chat_router
from app.config import settings
from app.log import setup_logging

# 初始化日志
setup_logging()

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
)


def _error_body(code: int, message: str, trace_id: str = "") -> dict:
    return {
        "code": code,
        "message": message,
        "data": None,
        "traceId": trace_id
    }


def _extract_trace_id(request: Request) -> str:
    tid = request.headers.get("x-trace-id", "")
    if tid and len(tid) == 16 and all(c in "0123456789abcdef" for c in tid):
        return tid
    # 生成一个 16 位的
    return secrets.token_hex(8)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """参数校验失败 → 统一信封格式，状态码 400"""
    trace_id = _extract_trace_id(request)
    # 取第一个错误信息
    msg = "参数格式错误"
    if exc.errors():
        first = exc.errors()[0]
        loc = ".".join(str(x) for x in first.get("loc", []))
        msg = f"{loc}: {first.get('msg', '参数错误')}"

    return JSONResponse(
        status_code=400,
        content=_error_body(1002, msg, trace_id),
        headers={"X-Trace-Id": trace_id}
    )


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """HTTP 异常处理 —— 把 detail 是 dict 的（我们自己抛的）转成信封"""
    trace_id = _extract_trace_id(request)

    if isinstance(exc.detail, dict) and "code" in exc.detail:
        # 我们自己构造的错误，已经是信封格式
        body = exc.detail
        if not body.get("traceId"):
            body["traceId"] = trace_id
        return JSONResponse(
            status_code=exc.status_code,
            content=body,
            headers={"X-Trace-Id": trace_id}
        )

    # 框架默认的 HTTP 异常
    code_map = {
        404: 1004,
        405: 1005,
        415: 1006,
    }
    code = code_map.get(exc.status_code, 9999)
    msg_map = {
        404: "接口不存在",
        405: "请求方法不支持",
        415: "请求内容类型不支持",
    }
    msg = msg_map.get(exc.status_code, str(exc.detail))

    return JSONResponse(
        status_code=exc.status_code,
        content=_error_body(code, msg, trace_id),
        headers={"X-Trace-Id": trace_id}
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """兜底异常"""
    trace_id = _extract_trace_id(request)
    return JSONResponse(
        status_code=500,
        content=_error_body(9999, "系统内部错误", trace_id),
        headers={"X-Trace-Id": trace_id}
    )


# 注册路由
app.include_router(health_router)
app.include_router(chat_router)


@app.middleware("http")
async def add_trace_id_header(request: Request, call_next):
    """确保每个响应都带 X-Trace-Id"""
    response = await call_next(request)
    if "x-trace-id" not in response.headers:
        trace_id = request.headers.get("x-trace-id") or secrets.token_hex(8)
        response.headers["X-Trace-Id"] = trace_id
    return response


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.app_env == "development"
    )
