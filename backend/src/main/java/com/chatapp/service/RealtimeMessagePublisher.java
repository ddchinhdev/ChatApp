package com.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class RealtimeMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(RealtimeMessagePublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ChatMessageCreatedEvent event) {
        for (String username : event.recipientUsernames()) {
            try {
                messagingTemplate.convertAndSendToUser(username, "/queue/messages", event.message());
            } catch (RuntimeException deliveryFailure) {
                log.warn("Realtime delivery failed eventId={} messageId={}", UUID.randomUUID(), event.message().id());
            }
        }
    }
}
