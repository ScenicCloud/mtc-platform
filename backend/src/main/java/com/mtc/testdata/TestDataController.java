package com.mtc.testdata;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.TestData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 测试数据控制器
 * 提供测试数据 CRUD 相关的 REST API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "测试数据管理", description = "测试数据的增删改查接口")
public class TestDataController {

    private final TestDataService testDataService;

    public TestDataController(TestDataService testDataService) {
        this.testDataService = testDataService;
    }

    /**
     * 按项目分页查询测试数据列表
     *
     * @param projectId 项目 ID
     * @param page      页码，默认 1
     * @param size      每页条数，默认 10
     */
    @GetMapping("/projects/{projectId}/test-data")
    @Operation(summary = "分页查询项目下的测试数据列表")
    public Result<Page<TestData>> getTestDataList(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<TestData> result = testDataService.getTestDataPage(projectId, page, size);
        return Result.ok(result);
    }

    /**
     * 获取测试数据详情
     *
     * @param id 测试数据 ID
     */
    @GetMapping("/test-data/{id}")
    @Operation(summary = "获取测试数据详情")
    public Result<TestData> getTestData(@PathVariable Long id) {
        TestData testData = testDataService.getTestDataById(id);
        return Result.ok(testData);
    }

    /**
     * 创建测试数据
     *
     * @param testData 测试数据信息
     */
    @PostMapping("/test-data")
    @Operation(summary = "创建测试数据")
    public Result<TestData> createTestData(@RequestBody TestData testData) {
        TestData created = testDataService.createTestData(testData);
        return Result.ok(created);
    }

    /**
     * 更新测试数据
     *
     * @param id       测试数据 ID
     * @param testData 更新内容
     */
    @PutMapping("/test-data/{id}")
    @Operation(summary = "更新测试数据")
    public Result<TestData> updateTestData(@PathVariable Long id, @RequestBody TestData testData) {
        TestData updated = testDataService.updateTestData(id, testData);
        return Result.ok(updated);
    }

    /**
     * 软删除测试数据
     *
     * @param id 测试数据 ID
     */
    @DeleteMapping("/test-data/{id}")
    @Operation(summary = "删除测试数据（软删除）")
    public Result<Void> deleteTestData(@PathVariable Long id) {
        testDataService.deleteTestData(id);
        return Result.ok();
    }
}
