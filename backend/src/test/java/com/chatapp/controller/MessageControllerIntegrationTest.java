package com.chatapp.controller;

import com.chatapp.entity.User;
import com.chatapp.entity.ConversationMember;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MessageControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository memberRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired RealtimeChatController realtimeChatController;

    @BeforeEach
    void cleanDatabase() {
        conversationRepository.findAll().forEach(conversation -> {
            conversation.setLastMessage(null);
            conversationRepository.save(conversation);
        });
        conversationRepository.flush();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void sendsAndPersistsMessageUsingAuthenticatedSender() throws Exception {
        Fixture fixture = fixture("send");
        String response = send(fixture.conversationId(), fixture.alice(), "  Xin chào Bob  ", 201);
        JsonNode message = objectMapper.readTree(response);

        assertThat(message.get("senderId").asLong()).isEqualTo(fixture.alice().id());
        assertThat(message.get("content").asText()).isEqualTo("Xin chào Bob");
        assertThat(message.get("createdAt").isTextual()).isTrue();
        assertThat(messageRepository.count()).isOne();

        String conversationResponse = mockMvc.perform(get("/api/conversations/{id}", fixture.conversationId())
                        .header("Authorization", bearer(fixture.alice().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastMessage.id").value(message.get("id").asLong()))
                .andExpect(jsonPath("$.lastMessage.content").value("Xin chào Bob"))
                .andReturn().getResponse().getContentAsString();
        LocalDateTime sentAt = LocalDateTime.parse(message.get("createdAt").asText()).truncatedTo(ChronoUnit.MICROS);
        LocalDateTime activityAt = LocalDateTime.parse(objectMapper.readTree(conversationResponse)
                .get("lastActivityAt").asText()).truncatedTo(ChronoUnit.MICROS);
        assertThat(activityAt).isEqualTo(sentAt);
    }

    @Test
    void readsLatestHistoryAndPaginatesWithoutDuplicatesOrGaps() throws Exception {
        Fixture fixture = fixture("page");
        List<Long> sentIds = new ArrayList<>();
        for (int index = 1; index <= 7; index++) {
            sentIds.add(objectMapper.readTree(send(fixture.conversationId(), fixture.alice(), "Tin " + index, 201)).get("id").asLong());
        }

        List<Long> receivedIds = new ArrayList<>();
        String cursor = null;
        boolean hasMore;
        do {
            var request = get("/api/conversations/{id}/messages", fixture.conversationId())
                    .header("Authorization", bearer(fixture.bob().token())).param("size", "3");
            if (cursor != null) request.param("cursor", cursor);
            JsonNode page = objectMapper.readTree(mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            page.get("messages").forEach(node -> receivedIds.add(node.get("id").asLong()));
            hasMore = page.get("hasMore").asBoolean();
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
        } while (hasMore);

        assertThat(receivedIds).hasSize(7);
        assertThat(new HashSet<>(receivedIds)).hasSize(7);
        assertThat(new HashSet<>(receivedIds)).isEqualTo(new HashSet<>(sentIds));
        assertThat(receivedIds).isSortedAccordingTo((left, right) -> Long.compare(right, left));
    }

    @Test
    void outsiderCannotSendOrReadMessages() throws Exception {
        Fixture fixture = fixture("deny");
        Account outsider = register("deny_outsider", "deny.outsider@example.com");

        send(fixture.conversationId(), outsider, "Không được phép", 403);
        mockMvc.perform(get("/api/conversations/{id}/messages", fixture.conversationId())
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void rejectsBlankAndOversizedContent() throws Exception {
        Fixture fixture = fixture("invalid");
        send(fixture.conversationId(), fixture.alice(), "   ", 400);
        send(fixture.conversationId(), fixture.alice(), "x".repeat(4001), 400);
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void realtimeSendPersistsAndRetryDoesNotCreateDuplicate() throws Exception {
        Fixture fixture = fixture("realtime");
        String clientMessageId = "client-" + UUID.randomUUID();
        RealtimeSendMessageRequest request = new RealtimeSendMessageRequest(
                fixture.conversationId(), "Realtime hello", clientMessageId);

        realtimeChatController.send(request, fixture.alice()::tokenUsername);
        realtimeChatController.send(request, fixture.alice()::tokenUsername);

        assertThat(messageRepository.count()).isOne();
        assertThat(messageRepository.findBySenderIdAndClientMessageId(fixture.alice().id(), clientMessageId))
                .isPresent().get().extracting(message -> message.getContent()).isEqualTo("Realtime hello");
    }

    @Test
    void outsiderCannotSendRealtimeAndOfflineReceiverDoesNotLosePersistedMessage() throws Exception {
        Fixture fixture = fixture("realtime_deny");
        Account outsider = register("realtime_outsider", "realtime.outsider@example.com");
        RealtimeSendMessageRequest denied = new RealtimeSendMessageRequest(
                fixture.conversationId(), "Denied", "client-" + UUID.randomUUID());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> realtimeChatController.send(denied, outsider::tokenUsername)))
                .isInstanceOf(BusinessException.class);

        RealtimeSendMessageRequest accepted = new RealtimeSendMessageRequest(
                fixture.conversationId(), "Stored while Bob is offline", "client-" + UUID.randomUUID());
        realtimeChatController.send(accepted, fixture.alice()::tokenUsername);
        assertThat(messageRepository.count()).isOne();
    }

    @Test
    void offlineReceiverSyncsAllMissedMessagesWithCursorAndNoForeignMessages() throws Exception {
        Fixture fixture = fixture("sync");
        Fixture foreign = fixture("foreign");
        List<Long> expected = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            expected.add(objectMapper.readTree(send(
                    fixture.conversationId(), fixture.alice(), "Missed " + index, 201)).get("id").asLong());
        }
        send(foreign.conversationId(), foreign.alice(), "Not visible", 201);

        JsonNode first = sync(fixture.bob(), 0, 2);
        assertThat(first.get("messages")).hasSize(2);
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        long cursor = first.get("nextAfterMessageId").asLong();
        JsonNode second = sync(fixture.bob(), cursor, 2);

        List<Long> received = new ArrayList<>();
        first.get("messages").forEach(message -> received.add(message.get("id").asLong()));
        second.get("messages").forEach(message -> received.add(message.get("id").asLong()));
        assertThat(received).containsExactlyElementsOf(expected);
        assertThat(new HashSet<>(received)).hasSize(3);
        assertThat(second.get("hasMore").asBoolean()).isFalse();

        Account outsider = register("sync_outsider", "sync.outsider@example.com");
        assertThat(sync(outsider, 0, 100).get("messages")).isEmpty();
    }

    @Test
    void calculatesConversationAndTotalUnreadThenMarksMessagesRead() throws Exception {
        Fixture fixture = fixture("unread");
        long firstId = objectMapper.readTree(send(fixture.conversationId(), fixture.alice(), "Một", 201)).get("id").asLong();
        long lastId = objectMapper.readTree(send(fixture.conversationId(), fixture.alice(), "Hai", 201)).get("id").asLong();

        mockMvc.perform(get("/api/conversations").header("Authorization", bearer(fixture.bob().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unreadCount").value(2));
        mockMvc.perform(get("/api/conversations/unread-count").header("Authorization", bearer(fixture.bob().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnread").value(2));

        markReceipt(fixture.conversationId(), fixture.bob(), "read", lastId, 200);

        mockMvc.perform(get("/api/conversations/{id}", fixture.conversationId())
                        .header("Authorization", bearer(fixture.bob().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
        ConversationMember bobMember = memberRepository.findByConversationIdAndUserId(
                fixture.conversationId(), fixture.bob().id()).orElseThrow();
        assertThat(bobMember.getLastReadMessageId()).isEqualTo(lastId);
        assertThat(bobMember.getLastDeliveredMessageId()).isEqualTo(lastId);
        assertThat(firstId).isLessThan(lastId);
    }

    @Test
    void readCursorOnlyMovesForwardAcrossSessions() throws Exception {
        Fixture fixture = fixture("forward");
        long firstId = objectMapper.readTree(send(fixture.conversationId(), fixture.alice(), "Cũ", 201)).get("id").asLong();
        long lastId = objectMapper.readTree(send(fixture.conversationId(), fixture.alice(), "Mới", 201)).get("id").asLong();

        markReceipt(fixture.conversationId(), fixture.bob(), "read", lastId, 200);
        markReceipt(fixture.conversationId(), fixture.bob(), "read", firstId, 200);

        ConversationMember bobMember = memberRepository.findByConversationIdAndUserId(
                fixture.conversationId(), fixture.bob().id()).orElseThrow();
        assertThat(bobMember.getLastReadMessageId()).isEqualTo(lastId);
        assertThat(bobMember.getLastDeliveredMessageId()).isEqualTo(lastId);
    }

    @Test
    void cannotAcknowledgeMessageFromAnotherConversation() throws Exception {
        Fixture allowed = fixture("receipt_allowed");
        Fixture foreign = fixture("receipt_foreign");
        long foreignMessageId = objectMapper.readTree(send(
                foreign.conversationId(), foreign.alice(), "Không thuộc conversation", 201)).get("id").asLong();

        markReceipt(allowed.conversationId(), allowed.bob(), "read", foreignMessageId, 404);
        markReceipt(foreign.conversationId(), allowed.bob(), "delivered", foreignMessageId, 403);
    }

    private Fixture fixture(String prefix) throws Exception {
        Account alice = register(prefix + "_alice", prefix + ".alice@example.com");
        Account bob = register(prefix + "_bob", prefix + ".bob@example.com");
        String response = mockMvc.perform(post("/api/conversations/direct/{userId}", bob.id())
                        .header("Authorization", bearer(alice.token())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Fixture(objectMapper.readTree(response).get("id").asLong(), alice, bob);
    }

    private String send(long conversationId, Account account, String content, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(account.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "content", content,
                                "clientMessageId", "rest-" + UUID.randomUUID(),
                                "senderId", -1))))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private JsonNode sync(Account account, long afterMessageId, int limit) throws Exception {
        String response = mockMvc.perform(get("/api/sync/messages")
                        .header("Authorization", bearer(account.token()))
                        .param("afterMessageId", String.valueOf(afterMessageId))
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private void markReceipt(long conversationId, Account account, String type, long messageId, int expectedStatus)
            throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/receipts/{type}", conversationId, type)
                        .header("Authorization", bearer(account.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("messageId", messageId))))
                .andExpect(status().is(expectedStatus));
    }

    private Account register(String username, String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(username, email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        User user = userRepository.findByUsername(username).orElseThrow();
        return new Account(user.getId(), objectMapper.readTree(response).get("accessToken").asText(), username);
    }

    private String bearer(String token) { return "Bearer " + token; }
    private record Account(Long id, String token, String username) {
        String tokenUsername() { return username; }
    }
    private record Fixture(Long conversationId, Account alice, Account bob) {}
}
