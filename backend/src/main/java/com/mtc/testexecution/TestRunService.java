package com.mtc.testexecution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.TestRun;
import com.mtc.entity.TestRunResult;
import com.mtc.mapper.TestRunMapper;
import com.mtc.mapper.TestRunResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 测试运行服务
 */
@Service
public class TestRunService {

    private static final Logger log = LoggerFactory.getLogger(TestRunService.class);
    private static final Long DEFAULT_CREATED_BY = 1L;

    private final TestRunMapper testRunMapper;
    private final TestRunResultMapper testRunResultMapper;

    public TestRunService(TestRunMapper testRunMapper, TestRunResultMapper testRunResultMapper) {
        this.testRunMapper = testRunMapper;
        this.testRunResultMapper = testRunResultMapper;
    }

    /**
     * 创建运行记录
     */
    @Transactional
    public TestRun createRun(Long projectId, Long scriptId, String scriptName, String baseUrl) {
        TestRun run = new TestRun();
        run.setProjectId(projectId);
        run.setScriptId(scriptId);
        run.setScriptName(scriptName);
        run.setBaseUrl(baseUrl);
        run.setStatus("pending");
        run.setTotalTests(0);
        run.setPassedTests(0);
        run.setFailedTests(0);
        run.setSkippedTests(0);
        run.setCreatedBy(DEFAULT_CREATED_BY);
        OffsetDateTime now = OffsetDateTime.now();
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        testRunMapper.insert(run);
        log.info("创建测试运行记录: id={}, script={}", run.getId(), scriptName);
        return run;
    }

    /**
     * 更新运行状态为运行中
     */
    public void markRunning(Long runId) {
        TestRun run = getById(runId);
        run.setStatus("running");
        run.setStartedAt(OffsetDateTime.now());
        run.setUpdatedAt(OffsetDateTime.now());
        testRunMapper.updateById(run);
    }

    /**
     * 更新运行状态为完成
     */
    @Transactional
    public void markCompleted(Long runId, PlaywrightExecutor.ExecutionResult execResult) {
        TestRun run = getById(runId);
        run.setStatus(execResult.getFailedTests() > 0 ? "failed" : "completed");
        run.setTotalTests(execResult.getTotalTests());
        run.setPassedTests(execResult.getPassedTests());
        run.setFailedTests(execResult.getFailedTests());
        run.setSkippedTests(execResult.getSkippedTests());
        run.setDurationMs(execResult.getDurationMs());
        run.setFinishedAt(OffsetDateTime.now());
        run.setUpdatedAt(OffsetDateTime.now());
        testRunMapper.updateById(run);

        // 保存用例级结果
        for (TestRunResult result : execResult.getResults()) {
            result.setRunId(runId);
            testRunResultMapper.insert(result);
        }

        log.info("测试运行完成: id={}, status={}, passed={}/{}",
                runId, run.getStatus(), run.getPassedTests(), run.getTotalTests());
    }

    /**
     * 更新运行状态为失败
     */
    public void markFailed(Long runId, String errorMessage) {
        TestRun run = getById(runId);
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(OffsetDateTime.now());
        run.setUpdatedAt(OffsetDateTime.now());
        testRunMapper.updateById(run);
        log.error("测试运行失败: id={}, error={}", runId, errorMessage);
    }

    /**
     * 根据 ID 获取运行记录
     */
    public TestRun getById(Long id) {
        TestRun run = testRunMapper.selectById(id);
        if (run == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "测试运行记录不存在");
        }
        return run;
    }

    /**
     * 分页查询项目下的运行记录
     */
    public Page<TestRun> getRunPage(Long projectId, int page, int size) {
        LambdaQueryWrapper<TestRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestRun::getProjectId, projectId)
                .orderByDesc(TestRun::getCreatedAt);
        return testRunMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取运行的用例结果列表
     */
    public List<TestRunResult> getRunResults(Long runId) {
        LambdaQueryWrapper<TestRunResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestRunResult::getRunId, runId)
                .orderByAsc(TestRunResult::getId);
        return testRunResultMapper.selectList(wrapper);
    }

    /**
     * 获取单个用例结果
     */
    public TestRunResult getResultById(Long resultId) {
        TestRunResult result = testRunResultMapper.selectById(resultId);
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "用例结果不存在");
        }
        return result;
    }
}
