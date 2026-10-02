package com.chatapp.service;

import com.chatapp.dto.message.ReceiptResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeReceiptPublisherTest {
    @Test
    void publishesReceiptStatusToSenderSessions() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        RealtimeReceiptPublisher publisher = new RealtimeReceiptPublisher(template);
        ReceiptResponse receipt = new ReceiptResponse(20L, 10L, "READ");

        publisher.publish(new ReceiptUpdatedEvent("alice", receipt));

        verify(template).convertAndSendToUser("alice", "/queue/receipts", receipt);
    }
}
