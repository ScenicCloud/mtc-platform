package com.mtc.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.Project;
import com.mtc.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 项目服务层
 * 提供项目的增删改查及分页查询功能
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private static final Long DEFAULT_CREATED_BY = 1L;

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 分页查询项目列表
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 名称/描述关键词（模糊匹配），可为 null
     * @return 分页结果
     */
    public Page<Project> getProjectPage(int page, int size, String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Project::getDeletedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Project::getName, keyword)
                    .or().like(Project::getDescription, keyword));
        }
        wrapper.orderByDesc(Project::getCreatedAt);
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取项目详情
     *
     * @param id 项目 ID
     * @return 项目实体
     * @throws BusinessException 项目不存在时抛出
     */
    public Project getProjectById(Long id) {
        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, id)
                        .isNull(Project::getDeletedAt)
        );
        if (project == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "项目不存在");
        }
        return project;
    }

    /**
     * 创建项目
     *
     * @param project 项目信息（name, description, status）
     * @return 创建后的项目实体（含自增 ID）
     */
    public Project createProject(Project project) {
        OffsetDateTime now = OffsetDateTime.now();
        project.setCreatedBy(DEFAULT_CREATED_BY);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        project.setDeletedAt(null);
        if (project.getStatus() == null || project.getStatus().isBlank()) {
            project.setStatus("active");
        }
        projectMapper.insert(project);
        log.info("项目创建成功: id={}, name={}", project.getId(), project.getName());
        return project;
    }

    /**
     * 更新项目
     *
     * @param id      项目 ID
     * @param project 更新字段（name, description, status）
     * @return 更新后的项目实体
     * @throws BusinessException 项目不存在时抛出
     */
    public Project updateProject(Long id, Project project) {
        Project existing = getProjectById(id);
        if (project.getName() != null) {
            existing.setName(project.getName());
        }
        if (project.getDescription() != null) {
            existing.setDescription(project.getDescription());
        }
        if (project.getStatus() != null) {
            existing.setStatus(project.getStatus());
        }
        existing.setUpdatedAt(OffsetDateTime.now());
        projectMapper.updateById(existing);
        log.info("项目更新成功: id={}", id);
        return existing;
    }

    /**
     * 软删除项目
     * 设置 deleted_at 字段为当前时间
     *
     * @param id 项目 ID
     * @throws BusinessException 项目不存在时抛出
     */
    public void deleteProject(Long id) {
        Project existing = getProjectById(id);
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        projectMapper.updateById(existing);
        log.info("项目删除成功: id={}", id);
    }
}
