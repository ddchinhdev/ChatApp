package com.chatapp.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GroupSystemMessagePublisher {
    private final ChatService chatService;

    public GroupSystemMessagePublisher(ChatService chatService) {
        this.chatService = chatService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GroupSystemMessageEvent event) {
        chatService.sendSystem(event.actorUsername(), event.conversationId(), event.content());
    }
}
