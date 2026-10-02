package com.chatapp.service;

import com.chatapp.dto.message.TypingEventResponse;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TypingService {
    static final long TYPING_TTL_SECONDS = 5;

    private final UserRepository userRepository;
    private final ConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TypingService(UserRepository userRepository, ConversationMemberRepository memberRepository,
                         SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public void publish(String username, Long conversationId, boolean typing) {
        User sender = userRepository.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, sender.getId())) {
            throw new BusinessException("You are not a member of this conversation", HttpStatus.FORBIDDEN);
        }
        TypingEventResponse event = new TypingEventResponse(conversationId, sender.getId(), sender.getDisplayName(),
                typing, typing ? Instant.now().plus(TYPING_TTL_SECONDS, ChronoUnit.SECONDS) : Instant.now());
        memberRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(ConversationMember::getUser)
                .filter(member -> !member.getId().equals(sender.getId()))
                .forEach(member -> messagingTemplate.convertAndSendToUser(
                        member.getUsername(), "/queue/typing", event));
    }
}
