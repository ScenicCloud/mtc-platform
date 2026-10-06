package com.mtc.testexecution;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.TestRun;
import com.mtc.entity.TestRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 测试执行控制器
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "测试执行", description = "测试脚本执行与结果查询")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;
    private final TestRunService testRunService;

    public TestExecutionController(
            TestExecutionService testExecutionService,
            TestRunService testRunService
    ) {
        this.testExecutionService = testExecutionService;
        this.testRunService = testRunService;
    }

    /**
     * 执行已保存的测试脚本
     */
    @PostMapping(value = "/test-scripts/{id}/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "执行测试脚本（SSE 流式输出）")
    public SseEmitter executeScript(
            @PathVariable Long id,
            @Parameter(description = "基础 URL，覆盖脚本默认值")
            @RequestParam(required = false) String baseUrl
    ) {
        return testExecutionService.executeScript(id, baseUrl);
    }

    /**
     * 直接执行脚本内容（不保存）
     */
    @PostMapping(value = "/test-runs/execute-content", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "直接执行脚本内容（SSE 流式输出）")
    public SseEmitter executeScriptContent(
            @RequestBody ExecuteContentRequest request
    ) {
        return testExecutionService.executeScriptContent(
                request.getProjectId(),
                request.getContent(),
                request.getName(),
                request.getBaseUrl()
        );
    }

    /**
     * 取消执行
     */
    @PostMapping("/test-runs/{id}/cancel")
    @Operation(summary = "取消测试执行")
    public Result<Void> cancelRun(@PathVariable Long id) {
        testExecutionService.cancelRun(id);
        return Result.ok();
    }

    /**
     * 分页查询运行记录
     */
    @GetMapping("/projects/{projectId}/test-runs")
    @Operation(summary = "分页查询项目下的测试运行记录")
    public Result<Page<TestRun>> getTestRuns(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size
    ) {
        Page<TestRun> result = testRunService.getRunPage(projectId, page, size);
        return Result.ok(result);
    }

    /**
     * 获取运行详情
     */
    @GetMapping("/test-runs/{id}")
    @Operation(summary = "获取测试运行详情")
    public Result<TestRun> getTestRun(@PathVariable Long id) {
        TestRun run = testRunService.getById(id);
        return Result.ok(run);
    }

    /**
     * 获取运行的用例结果列表
     */
    @GetMapping("/test-runs/{id}/results")
    @Operation(summary = "获取测试运行的用例结果列表")
    public Result<List<TestRunResult>> getRunResults(@PathVariable Long id) {
        List<TestRunResult> results = testRunService.getRunResults(id);
        return Result.ok(results);
    }

    /**
     * 获取单条用例结果详情
     */
    @GetMapping("/test-run-results/{id}")
    @Operation(summary = "获取用例执行结果详情")
    public Result<TestRunResult> getRunResult(@PathVariable Long id) {
        TestRunResult result = testRunService.getResultById(id);
        return Result.ok(result);
    }

    /**
     * 执行脚本内容请求体
     */
    public static class ExecuteContentRequest {
        private Long projectId;
        @NotBlank(message = "脚本内容不能为空")
        private String content;
        private String name;
        private String baseUrl;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
