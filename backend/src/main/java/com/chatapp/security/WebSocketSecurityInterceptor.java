package com.chatapp.security;

import com.chatapp.entity.User;
import com.chatapp.exception.WebSocketSecurityException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONVERSATION_TOPIC = "/topic/conversations/";
    private static final String CONVERSATION_APP = "/app/conversations/";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ConversationMemberRepository memberRepository;

    public WebSocketSecurityInterceptor(JwtService jwtService,
                                        UserRepository userRepository,
                                        ConversationMemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor);
        } else if (command == StompCommand.SEND) {
            authorizeSend(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, "Authorization", "authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new WebSocketSecurityException("AUTH_REQUIRED", "A Bearer token is required in the STOMP CONNECT headers");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !jwtService.isValid(token)) {
            throw new WebSocketSecurityException("INVALID_TOKEN", "The WebSocket access token is invalid or expired");
        }

        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new WebSocketSecurityException("INVALID_TOKEN", "The WebSocket user is unavailable"));
        ChatPrincipal principal = new ChatPrincipal(user.getId(), user.getUsername());
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        ChatPrincipal principal = requirePrincipal(accessor);
        String destination = requireDestination(accessor);
        if (destination.startsWith("/user/queue/")) return;
        Long conversationId = parseConversationId(destination, CONVERSATION_TOPIC);
        if (conversationId != null && memberRepository.existsByConversationIdAndUserId(conversationId, principal.userId())) return;
        throw new WebSocketSecurityException("SUBSCRIBE_FORBIDDEN", "Subscription to this destination is not allowed");
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        ChatPrincipal principal = requirePrincipal(accessor);
        String destination = requireDestination(accessor);
        if (destination.equals("/app/chat.send")
                || destination.equals("/app/chat.delivered")
                || destination.equals("/app/chat.read")
                || destination.equals("/app/chat.typing")) return;
        Long conversationId = parseConversationId(destination, CONVERSATION_APP);
        if (conversationId != null && memberRepository.existsByConversationIdAndUserId(conversationId, principal.userId())) return;
        throw new WebSocketSecurityException("SEND_FORBIDDEN", "Sending to this destination is not allowed");
    }

    private ChatPrincipal requirePrincipal(StompHeaderAccessor accessor) {
        Principal sessionPrincipal = accessor.getUser();
        Object principal = sessionPrincipal instanceof UsernamePasswordAuthenticationToken authentication
                ? authentication.getPrincipal() : sessionPrincipal;
        if (principal instanceof ChatPrincipal chatPrincipal) return chatPrincipal;
        throw new WebSocketSecurityException("AUTH_REQUIRED", "An authenticated WebSocket session is required");
    }

    private String requireDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            throw new WebSocketSecurityException("INVALID_DESTINATION", "A STOMP destination is required");
        }
        return destination;
    }

    private Long parseConversationId(String destination, String prefix) {
        if (!destination.startsWith(prefix)) return null;
        String remaining = destination.substring(prefix.length());
        String id = remaining.contains("/") ? remaining.substring(0, remaining.indexOf('/')) : remaining;
        try {
            return id.isBlank() ? null : Long.valueOf(id);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            String value = accessor.getFirstNativeHeader(name);
            if (value != null) return value;
        }
        return null;
    }
}
