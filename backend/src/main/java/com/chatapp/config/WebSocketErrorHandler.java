package com.chatapp.config;

import com.chatapp.exception.WebSocketSecurityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {
    private final ObjectMapper objectMapper;

    public WebSocketErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        WebSocketSecurityException security = findSecurityException(ex);
        String eventId = UUID.randomUUID().toString();
        String code = security == null ? "WEBSOCKET_ERROR" : security.getCode();
        String detail = security == null ? "The WebSocket request could not be processed" : security.getMessage();
        int status = code.equals("AUTH_REQUIRED") || code.equals("INVALID_TOKEN") ? 401 : 403;
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "timestamp", OffsetDateTime.now().toString(),
                    "status", status,
                    "code", code,
                    "message", detail,
                    "eventId", eventId));
            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.ERROR);
            accessor.setMessage(detail);
            accessor.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
            accessor.setContentLength(body.length);
            accessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(body, accessor.getMessageHeaders());
        } catch (Exception serializationFailure) {
            byte[] body = ("{\"code\":\"WEBSOCKET_ERROR\",\"eventId\":\"" + eventId + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return MessageBuilder.createMessage(body, new MessageHeaders(Map.of()));
        }
    }

    private WebSocketSecurityException findSecurityException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof WebSocketSecurityException security) return security;
            current = current.getCause();
        }
        return null;
    }
}
