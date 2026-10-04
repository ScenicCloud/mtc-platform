package com.mtc.testcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量创建测试用例请求
 */
@Schema(description = "批量创建测试用例请求")
public class BatchCreateTestCaseRequest {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    @NotEmpty(message = "测试用例列表不能为空")
    @Schema(description = "测试用例列表")
    private List<TestCaseItem> testCases;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<TestCaseItem> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseItem> testCases) {
        this.testCases = testCases;
    }

    /**
     * 单个测试用例项
     */
    @Schema(description = "测试用例项")
    public static class TestCaseItem {

        @Schema(description = "用例标题", example = "正常登录成功")
        private String title;

        @Schema(description = "模块", example = "用户登录")
        private String module;

        @Schema(description = "优先级：high/medium/low 或 P0/P1/P2/P3", example = "high")
        private String priority;

        @Schema(description = "前置条件", example = "用户已注册")
        private String precondition;

        @Schema(description = "测试步骤（字符串格式，支持换行）", example = "1. 打开页面\n2. 输入用户名")
        private String steps;

        @Schema(description = "预期结果", example = "登录成功")
        private String expectedResult;

        @Schema(description = "状态：draft/active/deprecated", example = "draft")
        private String status;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getPrecondition() {
            return precondition;
        }

        public void setPrecondition(String precondition) {
            this.precondition = precondition;
        }

        public String getSteps() {
            return steps;
        }

        public void setSteps(String steps) {
            this.steps = steps;
        }

        public String getExpectedResult() {
            return expectedResult;
        }

        public void setExpectedResult(String expectedResult) {
            this.expectedResult = expectedResult;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
