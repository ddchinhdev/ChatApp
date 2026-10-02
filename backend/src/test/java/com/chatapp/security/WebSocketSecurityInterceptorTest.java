package com.chatapp.security;

import com.chatapp.entity.User;
import com.chatapp.exception.WebSocketSecurityException;
import com.chatapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebSocketSecurityInterceptorTest {
    private static final String TEST_SECRET = "ChatAppTestSecretKeyThatIsLongEnoughForHS256Signing2026";

    @Autowired WebSocketSecurityInterceptor interceptor;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;

    private User user;
    private final MessageChannel channel = new ExecutorSubscribableChannel();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = new User();
        user.setUsername("socket_user");
        user.setEmail("socket@example.com");
        user.setPassword("not-used-by-websocket-test");
        user = userRepository.save(user);
    }

    @Test
    void validConnectAttachesAuthenticatedPrincipal() {
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, null,
                "Bearer " + jwtService.generateToken(user.getUsername()), null), channel);

        Principal principal = StompHeaderAccessor.wrap(result).getUser();
        assertThat(principal).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        Object authenticatedPrincipal = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        assertThat(authenticatedPrincipal).isEqualTo(new ChatPrincipal(user.getId(), user.getUsername()));
    }

    @Test
    void connectWithoutJwtIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), channel))
                .isInstanceOf(WebSocketSecurityException.class)
                .extracting(error -> ((WebSocketSecurityException) error).getCode())
                .isEqualTo("AUTH_REQUIRED");
    }

    @Test
    void invalidAndExpiredJwtAreRejected() {
        JwtService otherSigner = new JwtService("AnotherTestSecretKeyThatIsLongEnoughForHS256Signing2026", 60_000);
        JwtService expiredSigner = new JwtService(TEST_SECRET, -1);

        assertInvalid(otherSigner.generateToken(user.getUsername()));
        assertInvalid(expiredSigner.generateToken(user.getUsername()));
    }

    @Test
    void arbitraryOrOtherConversationSubscriptionIsRejected() {
        Principal principal = connectedPrincipal();

        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/user/another-user/queue/messages", null, principal), channel))
                .isInstanceOf(WebSocketSecurityException.class)
                .extracting(error -> ((WebSocketSecurityException) error).getCode())
                .isEqualTo("SUBSCRIBE_FORBIDDEN");
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/999999", null, principal), channel))
                .isInstanceOf(WebSocketSecurityException.class)
                .extracting(error -> ((WebSocketSecurityException) error).getCode())
                .isEqualTo("SUBSCRIBE_FORBIDDEN");
    }

    @Test
    void authenticatedSessionCanDisconnectCleanly() {
        Principal principal = connectedPrincipal();
        Message<?> disconnect = frame(StompCommand.DISCONNECT, null, null, principal);
        assertThat(interceptor.preSend(disconnect, channel)).isSameAs(disconnect);
    }

    @Test
    void authenticatedSessionMaySendTypingEvents() {
        Principal principal = connectedPrincipal();
        Message<?> typing = frame(StompCommand.SEND, "/app/chat.typing", null, principal);
        assertThat(interceptor.preSend(typing, channel)).isSameAs(typing);
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.CONNECT, null, "Bearer " + token, null), channel))
                .isInstanceOf(WebSocketSecurityException.class)
                .extracting(error -> ((WebSocketSecurityException) error).getCode())
                .isEqualTo("INVALID_TOKEN");
    }

    private Principal connectedPrincipal() {
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, null,
                "Bearer " + jwtService.generateToken(user.getUsername()), null), channel);
        return StompHeaderAccessor.wrap(result).getUser();
    }

    private Message<byte[]> frame(StompCommand command, String destination, String authorization, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("test-session");
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.setNativeHeader("Authorization", authorization);
        if (principal != null) accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
