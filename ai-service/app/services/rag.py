"""RAG 服务 — 文档分块、向量化、检索"""
import logging
import re
from typing import List, Tuple

import numpy as np

from app.config import settings
from app.services.llm import llm_service

logger = logging.getLogger(__name__)


def cosine_similarity(a: List[float], b: List[float]) -> float:
    """计算余弦相似度"""
    a_arr = np.array(a)
    b_arr = np.array(b)
    dot = np.dot(a_arr, b_arr)
    norm_a = np.linalg.norm(a_arr)
    norm_b = np.linalg.norm(b_arr)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(dot / (norm_a * norm_b))


def split_text(text: str, chunk_size: int = None, overlap: int = None) -> List[str]:
    """简单的文本分块：按段落 + 句子切分"""
    if chunk_size is None:
        chunk_size = settings.rag_chunk_size
    if overlap is None:
        overlap = settings.rag_chunk_overlap

    # 先按段落切
    paragraphs = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]

    chunks: List[str] = []
    current = ""

    for para in paragraphs:
        # 如果单个段落就超过 chunk_size，按句子切
        if len(para) > chunk_size:
            sentences = re.split(r"(?<=[。！？.!?])\s*", para)
            for sent in sentences:
                if not sent.strip():
                    continue
                if len(current) + len(sent) <= chunk_size:
                    current += sent
                else:
                    if current:
                        chunks.append(current.strip())
                    # overlap: 取当前块末尾 overlap 个字符作为下一块开头
                    if overlap > 0 and len(current) > overlap:
                        current = current[-overlap:] + sent
                    else:
                        current = sent
        else:
            if len(current) + len(para) + 2 <= chunk_size:
                current += ("\n" if current else "") + para
            else:
                if current:
                    chunks.append(current.strip())
                if overlap > 0 and len(current) > overlap:
                    current = current[-overlap:] + "\n" + para
                else:
                    current = para

    if current.strip():
        chunks.append(current.strip())

    return chunks


class RAGService:
    """RAG 检索服务（V0.1：内存向量检索，后续迁移到 pgvector）"""

    def __init__(self):
        # 内存存储：{doc_id: [(chunk_text, embedding), ...]}
        self._chunks: dict[int, List[Tuple[str, List[float]]]] = {}

    async def add_document(self, doc_id: int, text: str) -> int:
        """添加文档到向量库，返回分块数量"""
        chunks = split_text(text)
        if not chunks:
            return 0

        # 批量向量化
        embeddings = await llm_service.embed(chunks)
        self._chunks[doc_id] = list(zip(chunks, embeddings))
        logger.info(f"文档 {doc_id} 分块完成：{len(chunks)} 块")
        return len(chunks)

    async def search(
        self,
        query: str,
        doc_ids: List[int] = None,
        top_k: int = None,
    ) -> List[dict]:
        """
        检索相关文档块
        返回: [{text, score, doc_id, chunk_index}, ...]
        """
        if top_k is None:
            top_k = settings.rag_top_k

        # 查询向量化
        query_emb = await llm_service.embed_single(query)

        # 收集所有候选块
        candidates: List[Tuple[str, float, int, int]] = []  # (text, score, doc_id, idx)

        search_doc_ids = doc_ids if doc_ids else list(self._chunks.keys())

        for doc_id in search_doc_ids:
            if doc_id not in self._chunks:
                continue
            for idx, (text, emb) in enumerate(self._chunks[doc_id]):
                score = cosine_similarity(query_emb, emb)
                candidates.append((text, score, doc_id, idx))

        # 按相似度排序
        candidates.sort(key=lambda x: x[1], reverse=True)

        # 返回 top_k
        results = []
        for text, score, doc_id, idx in candidates[:top_k]:
            results.append({
                "text": text,
                "score": round(score, 4),
                "doc_id": doc_id,
                "chunk_index": idx,
            })

        return results

    def remove_document(self, doc_id: int) -> bool:
        """删除文档的向量数据"""
        if doc_id in self._chunks:
            del self._chunks[doc_id]
            return True
        return False

    def get_chunk_count(self, doc_id: int) -> int:
        """获取文档的分块数量"""
        return len(self._chunks.get(doc_id, []))


# 全局单例
rag_service = RAGService()
