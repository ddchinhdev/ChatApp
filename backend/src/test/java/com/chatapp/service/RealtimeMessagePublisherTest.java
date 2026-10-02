package com.chatapp.service;

import com.chatapp.dto.message.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeMessagePublisherTest {
    @Test
    void publishesToReceiverAndAllSenderSessions() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        RealtimeMessagePublisher publisher = new RealtimeMessagePublisher(template);
        MessageResponse message = new MessageResponse(
                10L, 20L, 1L, "alice", "Alice", "Hello", "client-1", "SENT", false, LocalDateTime.now());

        publisher.publish(new ChatMessageCreatedEvent(message, List.of("alice", "bob")));

        verify(template).convertAndSendToUser("alice", "/queue/messages", message);
        verify(template).convertAndSendToUser("bob", "/queue/messages", message);
    }
}
