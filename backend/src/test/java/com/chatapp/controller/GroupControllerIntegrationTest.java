package com.chatapp.controller;

import com.chatapp.entity.User;
import com.chatapp.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired ConversationRepository conversations;
    @Autowired ConversationMemberRepository members;
    @Autowired MessageRepository messages;

    @BeforeEach
    void clean() {
        conversations.findAll().forEach(conversation -> { conversation.setLastMessage(null); conversations.save(conversation); });
        conversations.flush();
        messages.deleteAll();
        members.deleteAll();
        conversations.deleteAll();
        users.deleteAll();
    }

    @Test
    void ownerAdminAndMemberPermissionsAreEnforced() throws Exception {
        Account owner = register("group_owner");
        Account admin = register("group_admin");
        Account member = register("group_member");
        Account invited = register("group_invited");
        long groupId = createGroup(owner, admin.id(), member.id());

        mockMvc.perform(post("/api/conversations/{id}/members/{userId}", groupId, invited.id())
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/conversations/{id}/members/{userId}/role", groupId, admin.id())
                        .header("Authorization", bearer(owner.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(post("/api/conversations/{id}/members/{userId}", groupId, invited.id())
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/conversations/{id}/members/{userId}", groupId, member.id())
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/conversations/{id}/members/{userId}", groupId, owner.id())
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isForbidden());
        assertThat(messages.findAll()).anyMatch(message -> Boolean.TRUE.equals(message.getSystem()));
    }

    @Test
    void supportsOwnershipTransferAndLeavingGroup() throws Exception {
        Account owner = register("leave_owner");
        Account successor = register("leave_successor");
        long groupId = createGroup(owner, successor.id());

        mockMvc.perform(post("/api/conversations/{id}/leave", groupId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/conversations/{id}/transfer-owner/{userId}", groupId, successor.id())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OWNER"));
        mockMvc.perform(post("/api/conversations/{id}/leave", groupId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNoContent());
        assertThat(members.existsByConversationIdAndUserId(groupId, owner.id())).isFalse();
        assertThat(members.findByConversationIdAndUserId(groupId, successor.id()).orElseThrow().getGroupRole().name())
                .isEqualTo("OWNER");
    }

    @Test
    void removedMemberCannotReadOrSendAndDuplicateMembershipIsRejected() throws Exception {
        Account owner = register("remove_owner");
        Account member = register("remove_member");
        long groupId = createGroup(owner, member.id());

        mockMvc.perform(post("/api/conversations/{id}/members/{userId}", groupId, member.id())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict());
        send(groupId, member, "Before removal", 201);
        mockMvc.perform(delete("/api/conversations/{id}/members/{userId}", groupId, member.id())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNoContent());
        send(groupId, member, "After removal", 403);
        mockMvc.perform(get("/api/conversations/{id}/messages", groupId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
    }

    private long createGroup(Account owner, Long... memberIds) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Nhóm kiểm thử", "memberIds", memberIds));
        String response = mockMvc.perform(post("/api/conversations/groups")
                        .header("Authorization", bearer(owner.token())).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void send(long groupId, Account account, String content, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/messages", groupId)
                        .header("Authorization", bearer(account.token())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", content, "clientMessageId", UUID.randomUUID().toString()))))
                .andExpect(status().is(expectedStatus));
    }

    private Account register(String username) throws Exception {
        String email = username + "@example.com";
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"Secret123\"}"
                                .formatted(username, email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        User user = users.findByUsername(username).orElseThrow();
        return new Account(user.getId(), objectMapper.readTree(response).get("accessToken").asText());
    }

    private String bearer(String token) { return "Bearer " + token; }
    private record Account(Long id, String token) {}
}
