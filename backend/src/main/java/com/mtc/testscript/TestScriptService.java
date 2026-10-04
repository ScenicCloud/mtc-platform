package com.mtc.testscript;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.TestScript;
import com.mtc.mapper.TestScriptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 测试脚本服务层
 * 提供测试脚本的增删改查及分页查询功能
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class TestScriptService {

    private static final Logger log = LoggerFactory.getLogger(TestScriptService.class);

    private static final Long DEFAULT_CREATED_BY = 1L;

    private final TestScriptMapper testScriptMapper;

    public TestScriptService(TestScriptMapper testScriptMapper) {
        this.testScriptMapper = testScriptMapper;
    }

    /**
     * 按项目分页查询测试脚本列表
     *
     * @param projectId 项目 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 分页结果
     */
    public Page<TestScript> getTestScriptPage(Long projectId, int page, int size) {
        LambdaQueryWrapper<TestScript> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestScript::getProjectId, projectId)
                .isNull(TestScript::getDeletedAt)
                .orderByDesc(TestScript::getCreatedAt);
        return testScriptMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取测试脚本详情
     *
     * @param id 测试脚本 ID
     * @return 测试脚本实体
     * @throws BusinessException 测试脚本不存在时抛出
     */
    public TestScript getTestScriptById(Long id) {
        TestScript testScript = testScriptMapper.selectOne(
                new LambdaQueryWrapper<TestScript>()
                        .eq(TestScript::getId, id)
                        .isNull(TestScript::getDeletedAt)
        );
        if (testScript == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "测试脚本不存在");
        }
        return testScript;
    }

    /**
     * 创建测试脚本
     *
     * @param testScript 测试脚本信息
     * @return 创建后的测试脚本实体（含自增 ID）
     */
    public TestScript createTestScript(TestScript testScript) {
        OffsetDateTime now = OffsetDateTime.now();
        testScript.setCreatedBy(DEFAULT_CREATED_BY);
        testScript.setCreatedAt(now);
        testScript.setUpdatedAt(now);
        testScript.setDeletedAt(null);
        if (testScript.getStatus() == null || testScript.getStatus().isBlank()) {
            testScript.setStatus("draft");
        }
        testScriptMapper.insert(testScript);
        log.info("测试脚本创建成功: id={}, name={}", testScript.getId(), testScript.getName());
        return testScript;
    }

    /**
     * 更新测试脚本
     *
     * @param id         测试脚本 ID
     * @param testScript 更新字段
     * @return 更新后的测试脚本实体
     * @throws BusinessException 测试脚本不存在时抛出
     */
    public TestScript updateTestScript(Long id, TestScript testScript) {
        TestScript existing = getTestScriptById(id);
        if (testScript.getName() != null) {
            existing.setName(testScript.getName());
        }
        if (testScript.getFramework() != null) {
            existing.setFramework(testScript.getFramework());
        }
        if (testScript.getLanguage() != null) {
            existing.setLanguage(testScript.getLanguage());
        }
        if (testScript.getContent() != null) {
            existing.setContent(testScript.getContent());
        }
        if (testScript.getStatus() != null) {
            existing.setStatus(testScript.getStatus());
        }
        if (testScript.getTestCaseId() != null) {
            existing.setTestCaseId(testScript.getTestCaseId());
        }
        existing.setUpdatedAt(OffsetDateTime.now());
        testScriptMapper.updateById(existing);
        log.info("测试脚本更新成功: id={}", id);
        return existing;
    }

    /**
     * 软删除测试脚本
     * 设置 deleted_at 字段为当前时间
     *
     * @param id 测试脚本 ID
     * @throws BusinessException 测试脚本不存在时抛出
     */
    public void deleteTestScript(Long id) {
        TestScript existing = getTestScriptById(id);
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        testScriptMapper.updateById(existing);
        log.info("测试脚本删除成功: id={}", id);
    }
}
