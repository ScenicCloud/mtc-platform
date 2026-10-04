package com.mtc.testcase;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.TestCase;
import com.mtc.testcase.dto.BatchCreateTestCaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 测试用例控制器
 * 提供测试用例 CRUD 及批量创建的 REST API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "测试用例管理", description = "测试用例的增删改查及批量创建接口")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    /**
     * 按项目分页查询测试用例列表
     *
     * @param projectId 项目 ID
     * @param page      页码，默认 1
     * @param size      每页条数，默认 10
     * @param module    模块筛选，可选
     * @param priority  优先级筛选，可选
     */
    @GetMapping("/projects/{projectId}/test-cases")
    @Operation(summary = "分页查询项目下的测试用例列表")
    public Result<Page<TestCase>> getTestCases(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "模块筛选") @RequestParam(required = false) String module,
            @Parameter(description = "优先级筛选") @RequestParam(required = false) String priority) {
        Page<TestCase> result = testCaseService.getTestCasePage(projectId, page, size, module, priority);
        return Result.ok(result);
    }

    /**
     * 获取测试用例详情
     *
     * @param id 测试用例 ID
     */
    @GetMapping("/test-cases/{id}")
    @Operation(summary = "获取测试用例详情")
    public Result<TestCase> getTestCase(@PathVariable Long id) {
        TestCase testCase = testCaseService.getTestCaseById(id);
        return Result.ok(testCase);
    }

    /**
     * 创建测试用例
     *
     * @param testCase 测试用例信息
     */
    @PostMapping("/test-cases")
    @Operation(summary = "创建测试用例")
    public Result<TestCase> createTestCase(@RequestBody TestCase testCase) {
        TestCase created = testCaseService.createTestCase(testCase);
        return Result.ok(created);
    }

    /**
     * 批量创建测试用例
     *
     * @param request 批量创建请求（包含项目ID和用例列表）
     */
    @PostMapping("/test-cases/batch")
    @Operation(summary = "批量创建测试用例")
    public Result<List<TestCase>> batchCreateTestCases(@Valid @RequestBody BatchCreateTestCaseRequest request) {
        List<TestCase> created = testCaseService.batchCreateTestCases(request.getProjectId(), request.getTestCases());
        return Result.ok(created);
    }

    /**
     * 更新测试用例
     *
     * @param id       测试用例 ID
     * @param testCase 更新内容
     */
    @PutMapping("/test-cases/{id}")
    @Operation(summary = "更新测试用例")
    public Result<TestCase> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        TestCase updated = testCaseService.updateTestCase(id, testCase);
        return Result.ok(updated);
    }

    /**
     * 软删除测试用例
     *
     * @param id 测试用例 ID
     */
    @DeleteMapping("/test-cases/{id}")
    @Operation(summary = "删除测试用例（软删除）")
    public Result<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return Result.ok();
    }
}
