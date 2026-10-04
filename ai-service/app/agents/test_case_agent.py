"""测试用例生成 Agent"""
import json
import logging
import re
from typing import AsyncGenerator, List, Optional

from app.services.llm import llm_service
from app.services.rag import rag_service

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """你是一位资深的软件测试专家，擅长根据需求描述生成高质量的测试用例。

你的任务：根据用户提供的需求描述，生成结构清晰、覆盖全面的测试用例。

测试用例生成原则：
1. 覆盖正常场景、异常场景、边界场景
2. 每个用例包含：用例标题、所属模块、优先级、前置条件、测试步骤、预期结果
3. 优先级分为：high（高）、medium（中）、low（低）
4. 用例要具体、可执行、可验证
5. 考虑等价类划分、边界值分析、错误推测等测试方法
6. 用例数量根据需求复杂度决定，一般 5-20 条

输出格式要求：
请用 JSON 数组格式输出，每个元素是一个测试用例，字段如下：
{
  "title": "用例标题",
  "module": "所属模块",
  "priority": "high/medium/low",
  "precondition": "前置条件",
  "steps": "测试步骤（编号，每行一步）",
  "expected_result": "预期结果"
}

只输出 JSON 数组，不要输出其他解释文字。"""


def parse_test_cases(content: str) -> List[dict]:
    """从 LLM 输出中解析测试用例 JSON"""
    # 尝试提取 JSON 数组
    json_match = re.search(r"\[[\s\S]*\]", content)
    if json_match:
        try:
            data = json.loads(json_match.group())
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            pass

    # 尝试逐行解析 JSON（流式输出不完整时用）
    try:
        # 去掉可能的 markdown 代码块标记
        cleaned = content.strip()
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```\w*\n", "", cleaned)
            cleaned = re.sub(r"\n```$", "", cleaned)
        data = json.loads(cleaned)
        if isinstance(data, list):
            return data
    except (json.JSONDecodeError, TypeError):
        pass

    logger.warning(f"无法解析测试用例 JSON，原始内容长度: {len(content)}")
    return []


class TestCaseAgent:
    """测试用例生成 Agent"""

    async def generate(
        self,
        requirement: str,
        doc_ids: List[int] = None,
        context: str = "",
    ) -> List[dict]:
        """生成测试用例（非流式）"""
        messages = []

        # RAG 检索上下文
        rag_context = ""
        if doc_ids:
            results = await rag_service.search(requirement, doc_ids=doc_ids)
            if results:
                rag_parts = []
                for i, r in enumerate(results, 1):
                    rag_parts.append(f"[参考资料{i}]\n{r['text']}")
                rag_context = "\n\n".join(rag_parts)

        user_msg = f"""需求描述：
{requirement}"""

        if rag_context:
            user_msg += f"\n\n参考文档：\n{rag_context}"

        if context:
            user_msg += f"\n\n补充上下文：\n{context}"

        messages.append({"role": "user", "content": user_msg})

        result = await llm_service.chat(
            messages,
            system_prompt=SYSTEM_PROMPT,
            temperature=0.7,
            max_tokens=4000,
        )

        test_cases = parse_test_cases(result)

        # 补充默认字段
        for tc in test_cases:
            if "type" not in tc:
                tc["type"] = "functional"
            if "status" not in tc:
                tc["status"] = "draft"
            if "source" not in tc:
                tc["source"] = "ai_generated"

        return test_cases

    async def generate_stream(
        self,
        requirement: str,
        doc_ids: List[int] = None,
        context: str = "",
    ) -> AsyncGenerator[str, None]:
        """流式生成测试用例（逐字输出原始内容）"""
        messages = []

        # RAG 检索
        rag_context = ""
        if doc_ids:
            results = await rag_service.search(requirement, doc_ids=doc_ids)
            if results:
                rag_parts = []
                for i, r in enumerate(results, 1):
                    rag_parts.append(f"[参考资料{i}]\n{r['text']}")
                rag_context = "\n\n".join(rag_parts)

        user_msg = f"""需求描述：
{requirement}"""

        if rag_context:
            user_msg += f"\n\n参考文档：\n{rag_context}"

        if context:
            user_msg += f"\n\n补充上下文：\n{context}"

        messages.append({"role": "user", "content": user_msg})

        async for token in llm_service.chat_stream(
            messages,
            system_prompt=SYSTEM_PROMPT,
            temperature=0.7,
            max_tokens=4000,
        ):
            yield token


# 全局单例
test_case_agent = TestCaseAgent()
