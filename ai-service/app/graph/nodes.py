"""
骨架阶段的占位节点 —— 回显节点
把用户输入的 message 逐字（按词）吐回去，模拟流式输出效果。
真业务上线后，替换为真实的 LLM 调用节点，流式 API 边界保持不变。
"""

import asyncio
import time
from typing import AsyncGenerator


async def echo_node(state: dict) -> dict:
    """回显节点：把 message 按字符拆分，逐段放入 tokens"""
    message = state.get("message", "")
    tokens = []

    # 模拟逐段输出：每 0.05 秒吐一个字符（中文按字）
    for ch in message:
        tokens.append(ch)
        await asyncio.sleep(0.05)

    return {"tokens": tokens, "done": True}


async def stream_echo(message: str) -> AsyncGenerator[str, None]:
    """直接流式生成 token，用于 SSE 直接输出（骨架阶段简单实现）"""
    for ch in message:
        yield ch
        await asyncio.sleep(0.05)
