package com.chatapp.controller;

import com.chatapp.dto.message.RealtimeSendMessageRequest;
import com.chatapp.dto.message.WebSocketErrorResponse;
import com.chatapp.dto.message.RealtimeReceiptRequest;
import com.chatapp.dto.message.TypingRequest;
import com.chatapp.exception.BusinessException;
import com.chatapp.service.ChatService;
import com.chatapp.service.ReceiptService;
import com.chatapp.service.TypingService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Controller
public class RealtimeChatController {
    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TypingService typingService;

    public RealtimeChatController(ChatService chatService, ReceiptService receiptService, TypingService typingService) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.typingService = typingService;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid RealtimeSendMessageRequest request, Principal principal) {
        chatService.send(principal.getName(), request.conversationId(), request.toSendRequest());
    }

    @MessageMapping("/chat.delivered")
    public void delivered(@Valid RealtimeReceiptRequest request, Principal principal) {
        receiptService.markDelivered(principal.getName(), request.conversationId(), request.messageId());
    }

    @MessageMapping("/chat.read")
    public void read(@Valid RealtimeReceiptRequest request, Principal principal) {
        receiptService.markRead(principal.getName(), request.conversationId(), request.messageId());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Valid TypingRequest request, Principal principal) {
        typingService.publish(principal.getName(), request.conversationId(), request.typing());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleMessageError(Exception exception) {
        String code = exception instanceof BusinessException ? "MESSAGE_REJECTED" : "INVALID_MESSAGE";
        String message = exception instanceof BusinessException
                ? exception.getMessage() : "The realtime message could not be processed";
        return new WebSocketErrorResponse(OffsetDateTime.now(), code, message, UUID.randomUUID().toString());
    }
}
