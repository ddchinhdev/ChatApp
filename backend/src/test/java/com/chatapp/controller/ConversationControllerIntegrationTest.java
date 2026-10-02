package com.chatapp.controller;

import com.chatapp.entity.User;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ConversationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository memberRepository;
    @Autowired ConversationService conversationService;

    @BeforeEach
    void cleanDatabase() {
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsDirectConversationAndReturnsSafeDto() throws Exception {
        Account alice = register("alice", "alice@example.com");
        Account bob = register("bob", "bob@example.com");

        mockMvc.perform(post("/api/conversations/direct/{userId}", bob.id())
                        .header("Authorization", bearer(alice.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.otherUser.id").value(bob.id()))
                .andExpect(jsonPath("$.otherUser.username").value("bob"))
                .andExpect(jsonPath("$.directConversationKey").doesNotExist())
                .andExpect(jsonPath("$.members").doesNotExist());
    }

    @Test
    void repeatedAndReversedRequestsReturnExactlyOneConversation() throws Exception {
        Account alice = register("alice2", "alice2@example.com");
        Account bob = register("bob2", "bob2@example.com");

        long firstId = create(alice, bob, 201);
        long repeatedId = create(alice, bob, 200);
        long reversedId = create(bob, alice, 200);

        assertThat(repeatedId).isEqualTo(firstId);
        assertThat(reversedId).isEqualTo(firstId);
        assertThat(conversationRepository.count()).isOne();
        assertThat(memberRepository.count()).isEqualTo(2);
    }

    @Test
    void simultaneousReversedCreatesConvergeOnOneConversation() throws Exception {
        Account alice = register("race_a", "race.a@example.com");
        Account bob = register("race_b", "race.b@example.com");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(() -> {
                start.await();
                return conversationService.createOrGetDirect("race_a", bob.id()).conversation().id();
            });
            Future<Long> second = executor.submit(() -> {
                start.await();
                return conversationService.createOrGetDirect("race_b", alice.id()).conversation().id();
            });
            start.countDown();

            assertThat(first.get()).isEqualTo(second.get());
        } finally {
            executor.shutdownNow();
        }
        assertThat(conversationRepository.count()).isOne();
        assertThat(memberRepository.count()).isEqualTo(2);
    }

    @Test
    void cannotCreateConversationWithSelf() throws Exception {
        Account alice = register("self_user", "self@example.com");
        mockMvc.perform(post("/api/conversations/direct/{userId}", alice.id())
                        .header("Authorization", bearer(alice.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot start a conversation with yourself"));
    }

    @Test
    void nonMemberCannotAccessConversation() throws Exception {
        Account alice = register("member_a", "member.a@example.com");
        Account bob = register("member_b", "member.b@example.com");
        Account outsider = register("outsider", "outsider@example.com");
        long id = create(alice, bob, 201);

        mockMvc.perform(get("/api/conversations/{id}", id)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listContainsOnlyMembershipsAndShowsOtherUser() throws Exception {
        Account alice = register("list_a", "list.a@example.com");
        Account bob = register("list_b", "list.b@example.com");
        Account outsider = register("list_c", "list.c@example.com");
        create(alice, bob, 201);

        mockMvc.perform(get("/api/conversations").header("Authorization", bearer(alice.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].otherUser.id").value(bob.id()));
        mockMvc.perform(get("/api/conversations").header("Authorization", bearer(outsider.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long create(Account from, Account to, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/conversations/direct/{userId}", to.id())
                        .header("Authorization", bearer(from.token())))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Account register(String username, String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"Secret123"}
                                """.formatted(username, email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        User user = userRepository.findByUsername(username).orElseThrow();
        return new Account(user.getId(), json.get("accessToken").asText());
    }

    private String bearer(String token) { return "Bearer " + token; }
    private record Account(Long id, String token) {}
}
