package com.mtc.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.Document;
import com.mtc.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 文档服务层
 * 提供文档的查询及删除功能
 * 文档的创建（上传）由专门的上传接口处理，V0.1 仅提供列表、详情、删除
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentMapper documentMapper;

    public DocumentService(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 按项目分页查询文档列表
     *
     * @param projectId 项目 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 分页结果
     */
    public Page<Document> getDocumentPage(Long projectId, int page, int size) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getProjectId, projectId)
                .isNull(Document::getDeletedAt)
                .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取文档详情
     *
     * @param id 文档 ID
     * @return 文档实体
     * @throws BusinessException 文档不存在时抛出
     */
    public Document getDocumentById(Long id) {
        Document document = documentMapper.selectOne(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getId, id)
                        .isNull(Document::getDeletedAt)
        );
        if (document == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "文档不存在");
        }
        return document;
    }

    /**
     * 软删除文档
     * 设置 deleted_at 字段为当前时间
     *
     * @param id 文档 ID
     * @throws BusinessException 文档不存在时抛出
     */
    public void deleteDocument(Long id) {
        Document existing = getDocumentById(id);
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        documentMapper.updateById(existing);
        log.info("文档删除成功: id={}, filename={}", id, existing.getFilename());
    }
}
