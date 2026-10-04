package com.mtc.testscript;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.TestScript;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 测试脚本控制器
 * 提供测试脚本 CRUD 相关的 REST API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "测试脚本管理", description = "测试脚本的增删改查接口")
public class TestScriptController {

    private final TestScriptService testScriptService;

    public TestScriptController(TestScriptService testScriptService) {
        this.testScriptService = testScriptService;
    }

    /**
     * 按项目分页查询测试脚本列表
     *
     * @param projectId 项目 ID
     * @param page      页码，默认 1
     * @param size      每页条数，默认 10
     */
    @GetMapping("/projects/{projectId}/test-scripts")
    @Operation(summary = "分页查询项目下的测试脚本列表")
    public Result<Page<TestScript>> getTestScripts(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<TestScript> result = testScriptService.getTestScriptPage(projectId, page, size);
        return Result.ok(result);
    }

    /**
     * 获取测试脚本详情
     *
     * @param id 测试脚本 ID
     */
    @GetMapping("/test-scripts/{id}")
    @Operation(summary = "获取测试脚本详情")
    public Result<TestScript> getTestScript(@PathVariable Long id) {
        TestScript testScript = testScriptService.getTestScriptById(id);
        return Result.ok(testScript);
    }

    /**
     * 创建测试脚本
     *
     * @param testScript 测试脚本信息
     */
    @PostMapping("/test-scripts")
    @Operation(summary = "创建测试脚本")
    public Result<TestScript> createTestScript(@RequestBody TestScript testScript) {
        TestScript created = testScriptService.createTestScript(testScript);
        return Result.ok(created);
    }

    /**
     * 更新测试脚本
     *
     * @param id         测试脚本 ID
     * @param testScript 更新内容
     */
    @PutMapping("/test-scripts/{id}")
    @Operation(summary = "更新测试脚本")
    public Result<TestScript> updateTestScript(@PathVariable Long id, @RequestBody TestScript testScript) {
        TestScript updated = testScriptService.updateTestScript(id, testScript);
        return Result.ok(updated);
    }

    /**
     * 软删除测试脚本
     *
     * @param id 测试脚本 ID
     */
    @DeleteMapping("/test-scripts/{id}")
    @Operation(summary = "删除测试脚本（软删除）")
    public Result<Void> deleteTestScript(@PathVariable Long id) {
        testScriptService.deleteTestScript(id);
        return Result.ok();
    }
}
