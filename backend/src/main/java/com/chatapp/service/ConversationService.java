package com.chatapp.service;

import com.chatapp.dto.conversation.ConversationResponse;
import com.chatapp.dto.conversation.ConversationUserResponse;
import com.chatapp.dto.conversation.LastMessageResponse;
import com.chatapp.entity.Conversation;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.User;
import com.chatapp.entity.Message;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.repository.MessageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.time.ZoneOffset;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final TransactionTemplate createTransaction;
    private final PresenceService presenceService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository memberRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               PresenceService presenceService,
                               PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
        this.createTransaction = new TransactionTemplate(transactionManager);
        this.createTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CreateResult createOrGetDirect(String username, Long targetUserId) {
        User currentUser = requireUser(username);
        if (currentUser.getId().equals(targetUserId)) {
            throw new BusinessException("You cannot start a conversation with yourself", HttpStatus.BAD_REQUEST);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new BusinessException("User not found", HttpStatus.NOT_FOUND);
        }

        String key = directKey(currentUser.getId(), targetUserId);
        Conversation existing = conversationRepository.findByDirectConversationKey(key).orElse(null);
        if (existing != null) return new CreateResult(toResponse(existing, currentUser.getId()), false);

        try {
            Conversation created = createTransaction.execute(status -> createDirect(currentUser.getId(), targetUserId, key));
            return new CreateResult(toResponse(created, currentUser.getId()), true);
        } catch (DataIntegrityViolationException duplicate) {
            Conversation winner = conversationRepository.findByDirectConversationKey(key)
                    .orElseThrow(() -> duplicate);
            return new CreateResult(toResponse(winner, currentUser.getId()), false);
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String username) {
        User currentUser = requireUser(username);
        return conversationRepository.findAllForUser(currentUser.getId()).stream()
                .map(conversation -> toResponse(conversation, currentUser.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(String username, Long conversationId) {
        User currentUser = requireUser(username);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, currentUser.getId())) {
            throw new BusinessException("You are not a member of this conversation", HttpStatus.FORBIDDEN);
        }
        return toResponse(conversation, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public long totalUnread(String username) {
        User currentUser = requireUser(username);
        return conversationRepository.findAllForUser(currentUser.getId()).stream()
                .mapToLong(conversation -> unreadCount(conversation.getId(), currentUser.getId()))
                .sum();
    }

    private Conversation createDirect(Long currentUserId, Long targetUserId, String key) {
        Conversation conversation = new Conversation();
        conversation.setGroup(false);
        conversation.setDirectConversationKey(key);
        conversation = conversationRepository.saveAndFlush(conversation);

        addMember(conversation, userRepository.getReferenceById(currentUserId));
        addMember(conversation, userRepository.getReferenceById(targetUserId));
        memberRepository.flush();
        return conversation;
    }

    private void addMember(Conversation conversation, User user) {
        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        memberRepository.save(member);
    }

    private ConversationResponse toResponse(Conversation conversation, Long currentUserId) {
        List<ConversationMember> members = memberRepository.findByConversationIdOrderByIdAsc(conversation.getId());
        Message lastMessage = conversation.getLastMessage();
        ConversationMember currentMember = members.stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Conversation membership is missing"));
        long unreadCount = messageRepository.countUnread(conversation.getId(), currentUserId,
                currentMember.getLastReadMessageId() == null ? 0L : currentMember.getLastReadMessageId());
        LastMessageResponse lastMessageResponse = lastMessage == null ? null : new LastMessageResponse(
                lastMessage.getId(), lastMessage.getSender().getId(), lastMessage.getContent(), lastMessage.getCreatedAt());
        ConversationUserResponse otherResponse = null;
        if (!Boolean.TRUE.equals(conversation.getGroup())) {
            User other = members.stream().map(ConversationMember::getUser)
                    .filter(user -> !user.getId().equals(currentUserId)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Direct conversation must have another member"));
            otherResponse = new ConversationUserResponse(other.getId(), other.getUsername(), other.getDisplayName(),
                    other.getAvatarUrl(), presenceService.isOnline(other.getId()), other.getLastSeenAt() == null ? null
                    : other.getLastSeenAt().toInstant(ZoneOffset.UTC));
        }
        return new ConversationResponse(
                conversation.getId(), Boolean.TRUE.equals(conversation.getGroup()) ? "GROUP" : "DIRECT", otherResponse,
                conversation.getCreatedAt(), conversation.getUpdatedAt(), conversation.getLastActivityAt(),
                lastMessageResponse, unreadCount, conversation.getName(), conversation.getAvatarUrl(),
                Boolean.TRUE.equals(conversation.getGroup()) ? currentMember.getGroupRole().name() : null, members.size());
    }

    private long unreadCount(Long conversationId, Long userId) {
        ConversationMember member = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalStateException("Conversation membership is missing"));
        return messageRepository.countUnread(conversationId, userId,
                member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    static String directKey(Long first, Long second) {
        return Math.min(first, second) + ":" + Math.max(first, second);
    }

    public record CreateResult(ConversationResponse conversation, boolean created) {}
}
