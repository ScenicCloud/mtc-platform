package com.mtc.testcase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.TestCase;
import com.mtc.mapper.TestCaseMapper;
import com.mtc.testcase.dto.BatchCreateTestCaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 测试用例服务层
 * 提供测试用例的增删改查、分页查询及批量创建功能
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);

    private static final Long DEFAULT_CREATED_BY = 1L;

    private final TestCaseMapper testCaseMapper;

    public TestCaseService(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    /**
     * 按项目分页查询测试用例列表
     *
     * @param projectId 项目 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @param module    模块筛选，可为 null
     * @param priority  优先级筛选，可为 null
     * @return 分页结果
     */
    public Page<TestCase> getTestCasePage(Long projectId, int page, int size, String module, String priority) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getProjectId, projectId)
                .isNull(TestCase::getDeletedAt);
        if (module != null && !module.isBlank()) {
            wrapper.eq(TestCase::getModule, module);
        }
        if (priority != null && !priority.isBlank()) {
            wrapper.eq(TestCase::getPriority, priority);
        }
        wrapper.orderByDesc(TestCase::getCreatedAt);
        return testCaseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取测试用例详情
     *
     * @param id 测试用例 ID
     * @return 测试用例实体
     * @throws BusinessException 测试用例不存在时抛出
     */
    public TestCase getTestCaseById(Long id) {
        TestCase testCase = testCaseMapper.selectOne(
                new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getId, id)
                        .isNull(TestCase::getDeletedAt)
        );
        if (testCase == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "测试用例不存在");
        }
        return testCase;
    }

    /**
     * 创建测试用例
     *
     * @param testCase 测试用例信息
     * @return 创建后的测试用例实体（含自增 ID）
     */
    public TestCase createTestCase(TestCase testCase) {
        OffsetDateTime now = OffsetDateTime.now();
        testCase.setCreatedBy(DEFAULT_CREATED_BY);
        testCase.setCreatedAt(now);
        testCase.setUpdatedAt(now);
        testCase.setDeletedAt(null);
        if (testCase.getStatus() == null || testCase.getStatus().isBlank()) {
            testCase.setStatus("draft");
        }
        if (testCase.getSource() == null || testCase.getSource().isBlank()) {
            testCase.setSource("manual");
        }
        testCaseMapper.insert(testCase);
        log.info("测试用例创建成功: id={}, title={}", testCase.getId(), testCase.getTitle());
        return testCase;
    }

    /**
     * 批量创建测试用例（通过 DTO）
     *
     * @param projectId 项目ID
     * @param items     测试用例项列表
     * @return 创建后的测试用例列表
     */
    public List<TestCase> batchCreateTestCases(Long projectId, List<BatchCreateTestCaseRequest.TestCaseItem> items) {
        OffsetDateTime now = OffsetDateTime.now();
        for (BatchCreateTestCaseRequest.TestCaseItem item : items) {
            TestCase testCase = new TestCase();
            testCase.setProjectId(projectId);
            testCase.setTitle(item.getTitle());
            testCase.setModule(item.getModule());
            testCase.setPriority(item.getPriority());
            testCase.setPrecondition(item.getPrecondition());
            // steps 可能是字符串（AI 生成）或数组，统一转为字符串存储
            testCase.setSteps(item.getSteps());
            testCase.setExpectedResult(item.getExpectedResult());
            testCase.setCreatedBy(DEFAULT_CREATED_BY);
            testCase.setCreatedAt(now);
            testCase.setUpdatedAt(now);
            testCase.setDeletedAt(null);
            testCase.setStatus(item.getStatus() != null && !item.getStatus().isBlank() ? item.getStatus() : "draft");
            testCase.setSource("ai_generated");
            testCase.setType("functional");
            testCaseMapper.insert(testCase);
            // 用 item 对象的引用保存回传的实体（方便外部获取 id）
            // 由于 item 是 DTO，我们需要通过另一种方式返回
        }
        // 重新查询返回（简单可靠）
        List<TestCase> result = testCaseMapper.selectList(
                new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getProjectId, projectId)
                        .orderByDesc(TestCase::getCreatedAt)
                        .last("limit " + items.size())
        );
        log.info("批量创建测试用例成功: projectId={}, count={}", projectId, items.size());
        return result;
    }

    /**
     * 批量创建测试用例（直接传实体列表，兼容旧接口）
     *
     * @param testCases 测试用例列表
     * @return 创建后的测试用例列表（含自增 ID）
     */
    public List<TestCase> batchCreateTestCasesDirect(List<TestCase> testCases) {
        OffsetDateTime now = OffsetDateTime.now();
        for (TestCase testCase : testCases) {
            testCase.setCreatedBy(DEFAULT_CREATED_BY);
            testCase.setCreatedAt(now);
            testCase.setUpdatedAt(now);
            testCase.setDeletedAt(null);
            if (testCase.getStatus() == null || testCase.getStatus().isBlank()) {
                testCase.setStatus("draft");
            }
            if (testCase.getSource() == null || testCase.getSource().isBlank()) {
                testCase.setSource("manual");
            }
            testCaseMapper.insert(testCase);
        }
        log.info("批量创建测试用例成功: count={}", testCases.size());
        return testCases;
    }

    /**
     * 更新测试用例
     *
     * @param id       测试用例 ID
     * @param testCase 更新字段
     * @return 更新后的测试用例实体
     * @throws BusinessException 测试用例不存在时抛出
     */
    public TestCase updateTestCase(Long id, TestCase testCase) {
        TestCase existing = getTestCaseById(id);
        if (testCase.getTitle() != null) {
            existing.setTitle(testCase.getTitle());
        }
        if (testCase.getModule() != null) {
            existing.setModule(testCase.getModule());
        }
        if (testCase.getPriority() != null) {
            existing.setPriority(testCase.getPriority());
        }
        if (testCase.getPrecondition() != null) {
            existing.setPrecondition(testCase.getPrecondition());
        }
        if (testCase.getSteps() != null) {
            existing.setSteps(testCase.getSteps());
        }
        if (testCase.getExpectedResult() != null) {
            existing.setExpectedResult(testCase.getExpectedResult());
        }
        if (testCase.getType() != null) {
            existing.setType(testCase.getType());
        }
        if (testCase.getStatus() != null) {
            existing.setStatus(testCase.getStatus());
        }
        if (testCase.getSource() != null) {
            existing.setSource(testCase.getSource());
        }
        if (testCase.getConversationId() != null) {
            existing.setConversationId(testCase.getConversationId());
        }
        existing.setUpdatedAt(OffsetDateTime.now());
        testCaseMapper.updateById(existing);
        log.info("测试用例更新成功: id={}", id);
        return existing;
    }

    /**
     * 软删除测试用例
     * 设置 deleted_at 字段为当前时间
     *
     * @param id 测试用例 ID
     * @throws BusinessException 测试用例不存在时抛出
     */
    public void deleteTestCase(Long id) {
        TestCase existing = getTestCaseById(id);
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        testCaseMapper.updateById(existing);
        log.info("测试用例删除成功: id={}", id);
    }
}
