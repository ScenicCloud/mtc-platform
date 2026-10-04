package com.mtc.document;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.entity.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 文档控制器
 * 提供文档列表查询、详情查看及删除的 REST API
 * 文档上传接口将在后续版本中实现
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "文档管理", description = "文档的查询及删除接口")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 按项目分页查询文档列表
     *
     * @param projectId 项目 ID
     * @param page      页码，默认 1
     * @param size      每页条数，默认 10
     */
    @GetMapping("/projects/{projectId}/documents")
    @Operation(summary = "分页查询项目下的文档列表")
    public Result<Page<Document>> getDocuments(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<Document> result = documentService.getDocumentPage(projectId, page, size);
        return Result.ok(result);
    }

    /**
     * 获取文档详情
     *
     * @param id 文档 ID
     */
    @GetMapping("/documents/{id}")
    @Operation(summary = "获取文档详情")
    public Result<Document> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id);
        return Result.ok(document);
    }

    /**
     * 软删除文档
     *
     * @param id 文档 ID
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档（软删除）")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.ok();
    }
}
