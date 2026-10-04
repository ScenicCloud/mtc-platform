"""文档 API — 上传、向量化、RAG 检索"""
import logging
import os
import secrets
from typing import List, Optional

from fastapi import APIRouter, Header, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field, ConfigDict

from app.config import settings
from app.services.rag import rag_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/documents", tags=["文档管理"])

# 确保上传目录存在
os.makedirs(settings.upload_dir, exist_ok=True)

ALLOWED_TYPES = {
    "text/plain": ".txt",
    "text/markdown": ".md",
    "application/json": ".json",
    "text/csv": ".csv",
}


def _validate_trace_id(x_trace_id: Optional[str], trace_id: str = "") -> str:
    if x_trace_id and len(x_trace_id) == 16:
        return x_trace_id
    if trace_id and len(trace_id) == 16:
        return trace_id
    return secrets.token_hex(8)


@router.post("/upload", summary="上传文档并向量化")
async def upload_document(
    project_id: int = Form(..., gt=0),
    file: UploadFile = File(...),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = _validate_trace_id(x_trace_id)

    # 校验文件类型
    content_type = file.content_type or ""
    if content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=400,
            detail={
                "code": 1002,
                "message": f"不支持的文件类型: {content_type}，支持 txt/md/json/csv",
                "data": None,
                "traceId": trace_id,
            },
        )

    # 校验文件大小
    file_bytes = await file.read()
    file_size = len(file_bytes)
    max_size = settings.max_file_size_mb * 1024 * 1024
    if file_size > max_size:
        raise HTTPException(
            status_code=400,
            detail={
                "code": 1002,
                "message": f"文件过大，最大支持 {settings.max_file_size_mb}MB",
                "data": None,
                "traceId": trace_id,
            },
        )

    # 解码文本内容
    try:
        text = file_bytes.decode("utf-8")
    except UnicodeDecodeError:
        try:
            text = file_bytes.decode("gbk")
        except UnicodeDecodeError:
            raise HTTPException(
                status_code=400,
                detail={
                    "code": 1002,
                    "message": "文件编码不支持，请使用 UTF-8 编码",
                    "data": None,
                    "traceId": trace_id,
                },
            )

    # 生成文件名和存储路径
    doc_id = abs(hash(f"{project_id}_{file.filename}_{secrets.token_hex(4)}")) % (10**9)
    ext = ALLOWED_TYPES.get(content_type, ".txt")
    storage_filename = f"{doc_id}_{file.filename}"
    storage_path = os.path.join(settings.upload_dir, storage_filename)

    # 保存文件
    with open(storage_path, "w", encoding="utf-8") as f:
        f.write(text)

    # 向量化（异步处理）
    chunk_count = 0
    try:
        chunk_count = await rag_service.add_document(doc_id, text)
        status = "processed"
    except Exception as e:
        logger.error(f"文档向量化失败: {e}", exc_info=True)
        status = "error"

    return {
        "code": 0,
        "message": "success",
        "data": {
            "id": doc_id,
            "project_id": project_id,
            "filename": file.filename,
            "file_type": content_type,
            "file_size": file_size,
            "storage_path": storage_path,
            "chunk_count": chunk_count,
            "status": status,
        },
        "traceId": trace_id,
    }


class SearchRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    query: str = Field(..., min_length=1, max_length=1000, description="查询内容")
    project_id: int = Field(..., gt=0, description="项目ID")
    doc_ids: List[int] = Field(default_factory=list, description="指定文档ID，空则搜全部")
    top_k: int = Field(default=5, ge=1, le=20, description="返回条数")
    trace_id: str = Field(default="", description="追踪ID")


@router.post("/search", summary="RAG 检索文档")
async def search_documents(
    req: SearchRequest,
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = _validate_trace_id(x_trace_id, req.trace_id)

    try:
        results = await rag_service.search(
            query=req.query,
            doc_ids=req.doc_ids if req.doc_ids else None,
            top_k=req.top_k,
        )
    except Exception as e:
        logger.error(f"RAG 检索失败: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail={
                "code": 9999,
                "message": f"检索失败: {str(e)}",
                "data": None,
                "traceId": trace_id,
            },
        )

    return {
        "code": 0,
        "message": "success",
        "data": {
            "results": results,
            "total": len(results),
        },
        "traceId": trace_id,
    }
