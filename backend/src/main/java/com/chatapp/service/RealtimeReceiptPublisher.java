package com.chatapp.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeReceiptPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeReceiptPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ReceiptUpdatedEvent event) {
        messagingTemplate.convertAndSendToUser(event.senderUsername(), "/queue/receipts", event.receipt());
    }
}
