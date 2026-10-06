"""测试脚本生成 Agent — 生成 Playwright TypeScript 自动化测试脚本"""
import json
import logging
import re
from typing import AsyncGenerator, List, Optional

from app.services.llm import llm_service

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """你是一位资深的自动化测试工程师，擅长使用 Playwright 编写高质量的 TypeScript 自动化测试脚本。

你的任务：根据测试用例描述，生成可执行的 Playwright TypeScript 测试脚本。

生成规范：
1. 使用 Playwright Test 框架（@playwright/test）
2. 使用 TypeScript
3. 每个 describe 块对应一个模块/功能
4. 每个 test 块对应一条测试用例
5. 使用 page 对象进行操作
6. 断言使用 expect
7. 选择器优先使用 role、text、label 等语义化选择器
8. 添加适当的注释
9. 代码结构清晰，可读性好
10. 考虑页面加载等待、元素可见性等稳定性问题

输出格式：
直接输出完整的 TypeScript 代码，用 ```typescript 代码块包裹。
代码顶部要有文件说明注释。
重要：代码必须有正确的换行和缩进，每个语句占一行，禁止压缩成单行！
重要：class 的 getter/setter 中 get/set 关键字和属性名之间必须有空格！"""


class TestScriptAgent:
    """测试脚本生成 Agent"""

    async def generate(
        self,
        test_cases: List[dict],
        module: str = "",
        base_url: str = "",
    ) -> dict:
        """生成测试脚本（非流式）"""
        # 构造测试用例描述
        case_descriptions = []
        for i, tc in enumerate(test_cases, 1):
            desc = f"""用例{i}: {tc.get('title', '')}
前置条件: {tc.get('precondition', '')}
测试步骤:
{tc.get('steps', '')}
预期结果: {tc.get('expected_result', '')}"""
            case_descriptions.append(desc)

        cases_text = "\n\n".join(case_descriptions)

        user_msg = f"""请根据以下测试用例生成 Playwright TypeScript 自动化测试脚本。

模块名称：{module or '未命名模块'}
被测页面基础URL：{base_url or 'https://example.com'}

测试用例：
{cases_text}

请生成完整的 .spec.ts 文件内容。"""

        result = await llm_service.chat(
            [{"role": "user", "content": user_msg}],
            system_prompt=SYSTEM_PROMPT,
            temperature=0.3,
            max_tokens=8000,
        )

        # 提取代码
        code = self._extract_code(result)

        return {
            "name": f"{module or 'test'}.spec.ts",
            "framework": "playwright",
            "language": "typescript",
            "content": code,
            "raw_output": result,
        }

    async def generate_stream(
        self,
        test_cases: List[dict],
        module: str = "",
        base_url: str = "",
    ) -> AsyncGenerator[str, None]:
        """流式生成测试脚本"""
        case_descriptions = []
        for i, tc in enumerate(test_cases, 1):
            desc = f"""用例{i}: {tc.get('title', '')}
前置条件: {tc.get('precondition', '')}
测试步骤:
{tc.get('steps', '')}
预期结果: {tc.get('expected_result', '')}"""
            case_descriptions.append(desc)

        cases_text = "\n\n".join(case_descriptions)

        user_msg = f"""请根据以下测试用例生成 Playwright TypeScript 自动化测试脚本。

模块名称：{module or '未命名模块'}
被测页面基础URL：{base_url or 'https://example.com'}

测试用例：
{cases_text}

请生成完整的 .spec.ts 文件内容。"""

        async for token in llm_service.chat_stream(
            [{"role": "user", "content": user_msg}],
            system_prompt=SYSTEM_PROMPT,
            temperature=0.3,
            max_tokens=8000,
        ):
            yield token

    def _extract_code(self, text: str) -> str:
        """从 markdown 输出中提取代码"""
        text = text.strip()
        # 标准格式：```typescript\n代码\n```
        match = re.search(r"```(?:typescript|ts|javascript|js)?\s*\n([\s\S]*?)\n```", text)
        if match:
            return match.group(1).strip()
        # 非标准格式：```typescript代码```（语言标识和代码在同一行，无换行）
        match = re.search(r"```(?:typescript|ts|javascript|js)([\s\S]*?)```", text)
        if match:
            return match.group(1).strip()
        # 只有 ``` 没有语言标识的情况
        match = re.search(r"```\n?([\s\S]*?)\n?```", text)
        if match:
            return match.group(1).strip()
        # 没有代码块，直接返回原文
        return text.strip()


# 全局单例
test_script_agent = TestScriptAgent()
