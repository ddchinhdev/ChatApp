package com.chatapp.config;

import com.chatapp.security.ChatPrincipal;
import com.chatapp.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketLifecycleLogger {
    private static final Logger log = LoggerFactory.getLogger(WebSocketLifecycleLogger.class);
    private final PresenceService presenceService;

    public WebSocketLifecycleLogger(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = userId(accessor.getUser());
        if (userId != null) {
            presenceService.connected(userId, accessor.getSessionId());
            log.info("WebSocket connected userId={} sessionId={}", userId, accessor.getSessionId());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = userId(event.getUser());
        if (userId != null) {
            boolean offline = presenceService.disconnected(userId, event.getSessionId());
            log.info("WebSocket disconnected userId={} sessionId={} offline={}", userId, event.getSessionId(), offline);
        }
        else log.info("WebSocket disconnected eventId={}", event.getSessionId());
    }

    private Long userId(Principal sessionPrincipal) {
        Object principal = sessionPrincipal instanceof UsernamePasswordAuthenticationToken authentication
                ? authentication.getPrincipal() : sessionPrincipal;
        return principal instanceof ChatPrincipal chatPrincipal ? chatPrincipal.userId() : null;
    }
}
