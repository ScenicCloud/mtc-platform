from typing import TypedDict, Annotated, Sequence
import operator


class AgentState(TypedDict):
    """LangGraph 状态定义 —— 骨架阶段只保留最小字段"""

    session_id: str
    user_id: int
    message: str
    # 流式输出的 token 序列
    tokens: Annotated[Sequence[str], operator.add]
    # 是否已完成
    done: bool
