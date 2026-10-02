package com.chatapp.controller;

import com.chatapp.entity.Role;
import com.chatapp.entity.User;
import com.chatapp.exception.WebSocketSecurityException;
import com.chatapp.repository.AdminAuditLogRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.WebSocketSecurityInterceptor;
import com.chatapp.dto.message.RealtimeSendMessageRequest;
import com.chatapp.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired AdminAuditLogRepository auditLogs;
    @Autowired WebSocketSecurityInterceptor webSocketSecurity;
    @Autowired RealtimeChatController realtimeChatController;

    @BeforeEach
    void clean() {
        auditLogs.deleteAll();
        users.deleteAll();
    }

    @Test
    void adminCanAccessStatsPaginatedUsersAndBasicDetail() throws Exception {
        Account admin = register("dashboard_admin");
        makeAdmin(admin.username());
        Account regular = register("dashboard_user");

        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users").value(2));
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin.token()))
                        .param("q", "dashboard").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/admin/users/{id}", regular.id()).header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(regular.username()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void regularUserReceivesForbiddenForEveryAdminApi() throws Exception {
        Account user = register("ordinary_user");
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(user.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(user.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", bearer(user.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void banningUserBlocksLoginAndReconnectAndCreatesAuditLog() throws Exception {
        Account admin = register("ban_admin");
        makeAdmin(admin.username());
        Account target = register("ban_target");

        mockMvc.perform(patch("/api/admin/users/{id}/status", target.id())
                        .header("Authorization", bearer(admin.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ban_target\",\"password\":\"Secret123\"}"))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> webSocketSecurity.preSend(connectFrame(target.token()), new ExecutorSubscribableChannel()))
                .isInstanceOf(WebSocketSecurityException.class);
        assertThatThrownBy(() -> realtimeChatController.send(
                new RealtimeSendMessageRequest(999L, "blocked", "blocked-client-id"), target::username))
                .isInstanceOf(BusinessException.class);
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].action").value("USER_BANNED"))
                .andExpect(jsonPath("$.content[0].targetUserId").value(target.id()));
        assertThat(auditLogs.count()).isOne();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    private org.springframework.messaging.Message<byte[]> connectFrame(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("admin-test-session");
        accessor.setNativeHeader("Authorization", bearer(token));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Account register(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"Secret123\"}"
                                .formatted(username, username)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        User user = users.findByUsername(username).orElseThrow();
        return new Account(user.getId(), username, body.get("accessToken").asText());
    }

    private void makeAdmin(String username) {
        User user = users.findByUsername(username).orElseThrow();
        user.setRole(Role.ADMIN);
        users.saveAndFlush(user);
    }

    private String bearer(String token) { return "Bearer " + token; }
    private record Account(Long id, String username, String token) {}
}
