package com.mtc.project;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.Project;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 项目管理控制器
 * 提供项目 CRUD 相关的 REST API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "项目管理", description = "项目的增删改查接口")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 分页查询项目列表
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 10
     * @param keyword 搜索关键词，可选
     */
    @GetMapping("/projects")
    @Operation(summary = "分页查询项目列表")
    public Result<Page<Project>> getProjects(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关键词（名称/描述模糊匹配）") @RequestParam(required = false) String keyword) {
        Page<Project> result = projectService.getProjectPage(page, size, keyword);
        return Result.ok(result);
    }

    /**
     * 获取项目详情
     *
     * @param id 项目 ID
     */
    @GetMapping("/projects/{id}")
    @Operation(summary = "获取项目详情")
    public Result<Project> getProject(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        return Result.ok(project);
    }

    /**
     * 创建项目
     *
     * @param project 项目信息
     */
    @PostMapping("/projects")
    @Operation(summary = "创建项目")
    public Result<Project> createProject(@RequestBody Project project) {
        Project created = projectService.createProject(project);
        return Result.ok(created);
    }

    /**
     * 更新项目
     *
     * @param id      项目 ID
     * @param project 更新内容
     */
    @PutMapping("/projects/{id}")
    @Operation(summary = "更新项目")
    public Result<Project> updateProject(@PathVariable Long id, @RequestBody Project project) {
        Project updated = projectService.updateProject(id, project);
        return Result.ok(updated);
    }

    /**
     * 软删除项目
     *
     * @param id 项目 ID
     */
    @DeleteMapping("/projects/{id}")
    @Operation(summary = "删除项目（软删除）")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.ok();
    }
}
