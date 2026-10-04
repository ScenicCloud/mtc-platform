package com.mtc.testdata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.TestData;
import com.mtc.mapper.TestDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 测试数据服务层
 * 提供测试数据的增删改查及分页查询功能
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class TestDataService {

    private static final Logger log = LoggerFactory.getLogger(TestDataService.class);

    private static final Long DEFAULT_CREATED_BY = 1L;

    private final TestDataMapper testDataMapper;

    public TestDataService(TestDataMapper testDataMapper) {
        this.testDataMapper = testDataMapper;
    }

    /**
     * 按项目分页查询测试数据列表
     *
     * @param projectId 项目 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 分页结果
     */
    public Page<TestData> getTestDataPage(Long projectId, int page, int size) {
        LambdaQueryWrapper<TestData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestData::getProjectId, projectId)
                .isNull(TestData::getDeletedAt)
                .orderByDesc(TestData::getCreatedAt);
        return testDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取测试数据详情
     *
     * @param id 测试数据 ID
     * @return 测试数据实体
     * @throws BusinessException 测试数据不存在时抛出
     */
    public TestData getTestDataById(Long id) {
        TestData testData = testDataMapper.selectOne(
                new LambdaQueryWrapper<TestData>()
                        .eq(TestData::getId, id)
                        .isNull(TestData::getDeletedAt)
        );
        if (testData == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "测试数据不存在");
        }
        return testData;
    }

    /**
     * 创建测试数据
     *
     * @param testData 测试数据信息
     * @return 创建后的测试数据实体（含自增 ID）
     */
    public TestData createTestData(TestData testData) {
        OffsetDateTime now = OffsetDateTime.now();
        testData.setCreatedBy(DEFAULT_CREATED_BY);
        testData.setCreatedAt(now);
        testData.setUpdatedAt(now);
        testData.setDeletedAt(null);
        testDataMapper.insert(testData);
        log.info("测试数据创建成功: id={}, name={}", testData.getId(), testData.getName());
        return testData;
    }

    /**
     * 更新测试数据
     *
     * @param id       测试数据 ID
     * @param testData 更新字段
     * @return 更新后的测试数据实体
     * @throws BusinessException 测试数据不存在时抛出
     */
    public TestData updateTestData(Long id, TestData testData) {
        TestData existing = getTestDataById(id);
        if (testData.getName() != null) {
            existing.setName(testData.getName());
        }
        if (testData.getDataType() != null) {
            existing.setDataType(testData.getDataType());
        }
        if (testData.getContent() != null) {
            existing.setContent(testData.getContent());
        }
        if (testData.getTestCaseId() != null) {
            existing.setTestCaseId(testData.getTestCaseId());
        }
        existing.setUpdatedAt(OffsetDateTime.now());
        testDataMapper.updateById(existing);
        log.info("测试数据更新成功: id={}", id);
        return existing;
    }

    /**
     * 软删除测试数据
     * 设置 deleted_at 字段为当前时间
     *
     * @param id 测试数据 ID
     * @throws BusinessException 测试数据不存在时抛出
     */
    public void deleteTestData(Long id) {
        TestData existing = getTestDataById(id);
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        testDataMapper.updateById(existing);
        log.info("测试数据删除成功: id={}", id);
    }
}
