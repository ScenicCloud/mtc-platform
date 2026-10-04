"""测试数据生成 Agent"""
import json
import logging
import re
from typing import AsyncGenerator, List, Optional

from app.services.llm import llm_service

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """你是一位资深的测试数据设计专家，擅长根据测试用例生成全面的测试数据。

你的任务：根据测试用例描述，生成覆盖各种场景的测试数据。

测试数据生成原则：
1. 覆盖正常数据、边界数据、异常数据、无效数据
2. 数据类型包括：字符串、数字、日期、布尔、特殊字符、空值等
3. 考虑等价类划分和边界值分析
4. 数据要真实、合理、有代表性
5. 标注每条数据的用途（测试什么场景）
6. 数量根据用例复杂度决定，一般 5-15 组

输出格式：
请用 JSON 格式输出，结构如下：
{
  "name": "测试数据名称",
  "description": "数据描述",
  "data_type": "json",
  "datasets": [
    {
      "name": "场景名称",
      "type": "normal/boundary/abnormal/invalid",
      "description": "数据说明",
      "data": { ...具体数据... }
    }
  ]
}

只输出 JSON，不要输出其他解释文字。"""


def parse_test_data(content: str) -> dict:
    """从 LLM 输出中解析测试数据 JSON"""
    # 提取 JSON 对象
    json_match = re.search(r"\{[\s\S]*\}", content)
    if json_match:
        try:
            data = json.loads(json_match.group())
            if isinstance(data, dict) and "datasets" in data:
                return data
        except json.JSONDecodeError:
            pass

    try:
        cleaned = content.strip()
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```\w*\n", "", cleaned)
            cleaned = re.sub(r"\n```$", "", cleaned)
        data = json.loads(cleaned)
        if isinstance(data, dict):
            return data
    except (json.JSONDecodeError, TypeError):
        pass

    logger.warning(f"无法解析测试数据 JSON，原始内容长度: {len(content)}")
    return {"name": "test-data", "description": "", "data_type": "json", "datasets": []}


class TestDataAgent:
    """测试数据生成 Agent"""

    async def generate(
        self,
        test_cases: List[dict],
        module: str = "",
    ) -> dict:
        """生成测试数据（非流式）"""
        case_descriptions = []
        for i, tc in enumerate(test_cases, 1):
            desc = f"""用例{i}: {tc.get('title', '')}
前置条件: {tc.get('precondition', '')}
测试步骤:
{tc.get('steps', '')}
预期结果: {tc.get('expected_result', '')}"""
            case_descriptions.append(desc)

        cases_text = "\n\n".join(case_descriptions)

        user_msg = f"""请根据以下测试用例生成测试数据。

模块：{module or '未命名模块'}

测试用例：
{cases_text}

请生成覆盖各种场景的测试数据。"""

        result = await llm_service.chat(
            [{"role": "user", "content": user_msg}],
            system_prompt=SYSTEM_PROMPT,
            temperature=0.5,
            max_tokens=4000,
        )

        data = parse_test_data(result)
        if "data_type" not in data:
            data["data_type"] = "json"

        return data

    async def generate_stream(
        self,
        test_cases: List[dict],
        module: str = "",
    ) -> AsyncGenerator[str, None]:
        """流式生成测试数据"""
        case_descriptions = []
        for i, tc in enumerate(test_cases, 1):
            desc = f"""用例{i}: {tc.get('title', '')}
前置条件: {tc.get('precondition', '')}
测试步骤:
{tc.get('steps', '')}
预期结果: {tc.get('expected_result', '')}"""
            case_descriptions.append(desc)

        cases_text = "\n\n".join(case_descriptions)

        user_msg = f"""请根据以下测试用例生成测试数据。

模块：{module or '未命名模块'}

测试用例：
{cases_text}

请生成覆盖各种场景的测试数据。"""

        async for token in llm_service.chat_stream(
            [{"role": "user", "content": user_msg}],
            system_prompt=SYSTEM_PROMPT,
            temperature=0.5,
            max_tokens=4000,
        ):
            yield token


# 全局单例
test_data_agent = TestDataAgent()
