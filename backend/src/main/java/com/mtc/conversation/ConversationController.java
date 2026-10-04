package com.mtc.conversation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtc.common.Result;
import com.mtc.conversation.dto.ConversationDetailVO;
import com.mtc.entity.Conversation;
import com.mtc.entity.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 会话控制器
 * 提供会话管理及消息追加的 REST API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "会话管理", description = "会话的增删查及消息追加接口")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 按项目分页查询会话列表
     *
     * @param projectId 项目 ID
     * @param page      页码，默认 1
     * @param size      每页条数，默认 10
     */
    @GetMapping("/projects/{projectId}/conversations")
    @Operation(summary = "分页查询项目下的会话列表")
    public Result<Page<Conversation>> getConversations(
            @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<Conversation> result = conversationService.getConversationPage(projectId, page, size);
        return Result.ok(result);
    }

    /**
     * 获取会话详情（含消息列表）
     *
     * @param id 会话 ID
     */
    @GetMapping("/conversations/{id}")
    @Operation(summary = "获取会话详情（含消息列表）")
    public Result<ConversationDetailVO> getConversation(@PathVariable Long id) {
        ConversationDetailVO detail = conversationService.getConversationDetail(id);
        return Result.ok(detail);
    }

    /**
     * 创建会话
     *
     * @param conversation 会话信息
     */
    @PostMapping("/conversations")
    @Operation(summary = "创建会话")
    public Result<Conversation> createConversation(@RequestBody Conversation conversation) {
        Conversation created = conversationService.createConversation(conversation);
        return Result.ok(created);
    }

    /**
     * 删除会话
     *
     * @param id 会话 ID
     */
    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除会话（软删除）")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return Result.ok();
    }

    /**
     * 向会话追加消息
     *
     * @param id      会话 ID
     * @param message 消息内容
     */
    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "追加消息到会话")
    public Result<Message> appendMessage(@PathVariable Long id, @RequestBody Message message) {
        Message created = conversationService.appendMessage(id, message);
        return Result.ok(created);
    }
}
