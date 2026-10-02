from fastapi import APIRouter, Header

router = APIRouter(tags=["健康检查"])


@router.get("/health", summary="健康检查")
async def health(x_trace_id: str | None = Header(default=None)):
    return {"status": "ok"}
