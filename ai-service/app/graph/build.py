"""
构建 LangGraph 图 —— 骨架阶段只有一个回显节点
真业务上线后，增加更多节点（RAG、工具调用、质量评审等），
但图的对外接口（输入/输出/流式事件）保持不变。
"""

from langgraph.graph import StateGraph, END
from app.graph.state import AgentState
from app.graph.nodes import echo_node


def build_graph():
    """构建最小回显图"""
    workflow = StateGraph(AgentState)

    # 只有一个节点：回显
    workflow.add_node("echo", echo_node)

    # 入口 → echo → END
    workflow.set_entry_point("echo")
    workflow.add_edge("echo", END)

    # 骨架阶段用内存检查点，不依赖数据库
    return workflow.compile()


# 全局图实例
graph = build_graph()
