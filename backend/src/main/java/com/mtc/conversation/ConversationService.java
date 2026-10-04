package com.mtc.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.conversation.dto.ConversationDetailVO;
import com.mtc.entity.Conversation;
import com.mtc.entity.Message;
import com.mtc.mapper.ConversationMapper;
import com.mtc.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 会话服务层
 * 提供会话的增删查、消息追加等功能
 * 软删除基于 deleted_at 字段实现
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private static final Long DEFAULT_CREATED_BY = 1L;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationService(ConversationMapper conversationMapper, MessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 按项目分页查询会话列表
     *
     * @param projectId 项目 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 分页结果
     */
    public Page<Conversation> getConversationPage(Long projectId, int page, int size) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getProjectId, projectId)
                .isNull(Conversation::getDeletedAt)
                .orderByDesc(Conversation::getUpdatedAt);
        return conversationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据 ID 获取会话详情（含消息列表）
     *
     * @param id 会话 ID
     * @return 会话详情（含消息列表）
     * @throws BusinessException 会话不存在时抛出
     */
    public ConversationDetailVO getConversationDetail(Long id) {
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, id)
                        .isNull(Conversation::getDeletedAt)
        );
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "会话不存在");
        }

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, id)
                        .orderByAsc(Message::getCreatedAt)
        );

        return new ConversationDetailVO(conversation, messages);
    }

    /**
     * 创建会话
     *
     * @param conversation 会话信息
     * @return 创建后的会话实体（含自增 ID）
     */
    public Conversation createConversation(Conversation conversation) {
        OffsetDateTime now = OffsetDateTime.now();
        conversation.setCreatedBy(DEFAULT_CREATED_BY);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setDeletedAt(null);
        if (conversation.getType() == null || conversation.getType().isBlank()) {
            conversation.setType("normal");
        }
        conversationMapper.insert(conversation);
        log.info("会话创建成功: id={}, title={}", conversation.getId(), conversation.getTitle());
        return conversation;
    }

    /**
     * 删除会话（软删除）
     * 同时不删除关联消息（保留历史数据）
     *
     * @param id 会话 ID
     * @throws BusinessException 会话不存在时抛出
     */
    public void deleteConversation(Long id) {
        Conversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, id)
                        .isNull(Conversation::getDeletedAt)
        );
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "会话不存在");
        }
        existing.setDeletedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        conversationMapper.updateById(existing);
        log.info("会话删除成功: id={}", id);
    }

    /**
     * 向会话追加消息
     *
     * @param conversationId 会话 ID
     * @param message        消息内容（role, content, tokenCount）
     * @return 创建后的消息实体
     * @throws BusinessException 会话不存在时抛出
     */
    public Message appendMessage(Long conversationId, Message message) {
        // 验证会话存在
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .isNull(Conversation::getDeletedAt)
        );
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "会话不存在");
        }

        OffsetDateTime now = OffsetDateTime.now();
        message.setConversationId(conversationId);
        message.setCreatedAt(now);
        if (message.getTokenCount() == null) {
            message.setTokenCount(0);
        }
        messageMapper.insert(message);

        // 更新会话的更新时间
        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);

        log.info("消息追加成功: conversationId={}, messageId={}, role={}",
                conversationId, message.getId(), message.getRole());
        return message;
    }
}
