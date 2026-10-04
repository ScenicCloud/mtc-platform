package com.mtc.conversation.dto;

import com.mtc.entity.Conversation;
import com.mtc.entity.Message;

import java.util.List;

/**
 * 会话详情 VO
 * 包含会话基本信息及消息列表
 */
public class ConversationDetailVO {

    private Conversation conversation;
    private List<Message> messages;

    public ConversationDetailVO() {
    }

    public ConversationDetailVO(Conversation conversation, List<Message> messages) {
        this.conversation = conversation;
        this.messages = messages;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
