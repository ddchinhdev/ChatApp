package com.chatapp.service;

import com.chatapp.dto.message.MessagePageResponse;
import com.chatapp.dto.message.MessageResponse;
import com.chatapp.dto.message.MessageSyncResponse;
import com.chatapp.dto.message.SendMessageRequest;
import com.chatapp.entity.Conversation;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate sendTransaction;

    public ChatService(MessageRepository messageRepository,
                       ConversationRepository conversationRepository,
                       ConversationMemberRepository memberRepository,
                       UserRepository userRepository,
                       ApplicationEventPublisher eventPublisher,
                       PlatformTransactionManager transactionManager) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.sendTransaction = new TransactionTemplate(transactionManager);
        this.sendTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MessageResponse send(String username, Long conversationId, SendMessageRequest request) {
        User sender = requireUser(username);
        Message existing = messageRepository.findBySenderIdAndClientMessageId(sender.getId(), request.clientMessageId())
                .orElse(null);
        if (existing != null) {
            verifySameRetry(existing, conversationId, request.content());
            return toResponse(existing, sender.getId());
        }
        try {
            return sendTransaction.execute(status -> persistAndSchedulePublish(sender.getId(), conversationId, request, false));
        } catch (DataIntegrityViolationException duplicate) {
            Message winner = messageRepository.findBySenderIdAndClientMessageId(sender.getId(), request.clientMessageId())
                    .orElseThrow(() -> duplicate);
            verifySameRetry(winner, conversationId, request.content());
            return toResponse(winner, sender.getId());
        }
    }

    public MessageResponse sendSystem(String username, Long conversationId, String content) {
        User sender = requireUser(username);
        SendMessageRequest request = new SendMessageRequest(content, "system-" + UUID.randomUUID());
        return sendTransaction.execute(status -> persistAndSchedulePublish(sender.getId(), conversationId, request, true));
    }

    @Transactional(readOnly = true)
    public MessagePageResponse history(String username, Long conversationId, String cursor, int size) {
        User user = requireUser(username);
        requireMemberConversation(conversationId, user.getId(), false);
        Cursor decoded = decodeCursor(cursor);
        List<Message> result = messageRepository.findHistory(
                conversationId, decoded.time(), decoded.id(), PageRequest.of(0, size + 1));
        boolean hasMore = result.size() > size;
        List<Message> page = hasMore ? result.subList(0, size) : result;
        String nextCursor = hasMore && !page.isEmpty() ? encodeCursor(page.get(page.size() - 1)) : null;
        return new MessagePageResponse(page.stream().map(message -> toResponse(message, user.getId())).toList(), nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public MessageSyncResponse sync(String username, Long afterMessageId, int limit) {
        User user = requireUser(username);
        List<Message> result = messageRepository.findMessagesForSync(
                user.getId(), afterMessageId, PageRequest.of(0, limit + 1));
        boolean hasMore = result.size() > limit;
        List<Message> page = hasMore ? result.subList(0, limit) : result;
        Long nextCursor = page.isEmpty() ? afterMessageId : page.get(page.size() - 1).getId();
        return new MessageSyncResponse(page.stream().map(message -> toResponse(message, user.getId())).toList(), nextCursor, hasMore);
    }

    private MessageResponse persistAndSchedulePublish(Long senderId, Long conversationId, SendMessageRequest request,
                                                      boolean system) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        Conversation conversation = requireMemberConversation(conversationId, senderId, true);
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.content().trim());
        message.setClientMessageId(request.clientMessageId());
        message.setSystem(system);
        message = messageRepository.saveAndFlush(message);

        conversation.setLastMessage(message);
        conversation.setLastActivityAt(message.getCreatedAt());
        conversation.setUpdatedAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        MessageResponse response = toResponse(message, senderId);
        List<String> recipients = memberRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(ConversationMember::getUser).map(User::getUsername).toList();
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(response, recipients));
        return response;
    }

    private void verifySameRetry(Message existing, Long conversationId, String content) {
        if (!existing.getConversation().getId().equals(conversationId)
                || !existing.getContent().equals(content.trim())) {
            throw new BusinessException("clientMessageId was already used for a different message", HttpStatus.CONFLICT);
        }
    }

    private Conversation requireMemberConversation(Long conversationId, Long userId, boolean forUpdate) {
        Conversation conversation = (forUpdate
                ? conversationRepository.findByIdForUpdate(conversationId)
                : conversationRepository.findById(conversationId))
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BusinessException("You are not a member of this conversation", HttpStatus.FORBIDDEN);
        }
        return conversation;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private MessageResponse toResponse(Message message, Long viewerId) {
        User sender = message.getSender();
        String receiptStatus = receiptStatus(message, viewerId);
        return new MessageResponse(message.getId(), message.getConversation().getId(), sender.getId(),
                sender.getUsername(), sender.getDisplayName(), message.getContent(), message.getClientMessageId(),
                receiptStatus, Boolean.TRUE.equals(message.getSystem()), message.getCreatedAt());
    }

    private String receiptStatus(Message message, Long viewerId) {
        if (!message.getSender().getId().equals(viewerId)) return "SENT";
        return memberRepository.findByConversationIdOrderByIdAsc(message.getConversation().getId()).stream()
                .filter(member -> !member.getUser().getId().equals(viewerId))
                .findFirst()
                .map(member -> {
                    if (cursorAtLeast(member.getLastReadMessageId(), message.getId())) return "READ";
                    if (cursorAtLeast(member.getLastDeliveredMessageId(), message.getId())) return "DELIVERED";
                    return "SENT";
                }).orElse("SENT");
    }

    private boolean cursorAtLeast(Long cursor, Long messageId) {
        return cursor != null && cursor >= messageId;
    }

    private String encodeCursor(Message message) {
        String value = message.getCreatedAt() + "|" + message.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new Cursor(null, null);
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = value.lastIndexOf('|');
            return new Cursor(LocalDateTime.parse(value.substring(0, separator)), Long.parseLong(value.substring(separator + 1)));
        } catch (RuntimeException invalid) {
            throw new BusinessException("Invalid message cursor", HttpStatus.BAD_REQUEST);
        }
    }

    private record Cursor(LocalDateTime time, Long id) {}
}
