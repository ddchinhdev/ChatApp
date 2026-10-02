package com.chatapp.service;

import com.chatapp.dto.message.ReceiptResponse;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReceiptService(UserRepository userRepository, MessageRepository messageRepository,
                          ConversationMemberRepository memberRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReceiptResponse markDelivered(String username, Long conversationId, Long messageId) {
        return advance(username, conversationId, messageId, false);
    }

    @Transactional
    public ReceiptResponse markRead(String username, Long conversationId, Long messageId) {
        return advance(username, conversationId, messageId, true);
    }

    private ReceiptResponse advance(String username, Long conversationId, Long messageId, boolean read) {
        User receiver = userRepository.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        ConversationMember member = memberRepository.findForUpdate(conversationId, receiver.getId())
                .orElseThrow(() -> new BusinessException("You are not a member of this conversation", HttpStatus.FORBIDDEN));
        Message message = messageRepository.findById(messageId)
                .filter(candidate -> candidate.getConversation().getId().equals(conversationId))
                .orElseThrow(() -> new BusinessException("Message not found in this conversation", HttpStatus.NOT_FOUND));
        if (message.getSender().getId().equals(receiver.getId())) {
            throw new BusinessException("A sender cannot acknowledge their own message", HttpStatus.BAD_REQUEST);
        }

        boolean changed;
        String status;
        if (read) {
            changed = advanceRead(member, messageId);
            status = "READ";
        } else {
            changed = advanceDelivered(member, messageId);
            status = "DELIVERED";
        }
        if (changed) {
            memberRepository.save(member);
            ReceiptResponse receipt = new ReceiptResponse(conversationId, messageId, status);
            eventPublisher.publishEvent(new ReceiptUpdatedEvent(message.getSender().getUsername(), receipt));
            return receipt;
        }
        long effective = read ? value(member.getLastReadMessageId()) : value(member.getLastDeliveredMessageId());
        return new ReceiptResponse(conversationId, effective, status);
    }

    private boolean advanceDelivered(ConversationMember member, Long messageId) {
        if (value(member.getLastDeliveredMessageId()) >= messageId) return false;
        member.setLastDeliveredMessageId(messageId);
        return true;
    }

    private boolean advanceRead(ConversationMember member, Long messageId) {
        boolean changed = false;
        if (value(member.getLastReadMessageId()) < messageId) {
            member.setLastReadMessageId(messageId);
            changed = true;
        }
        if (value(member.getLastDeliveredMessageId()) < messageId) {
            member.setLastDeliveredMessageId(messageId);
            changed = true;
        }
        return changed;
    }

    private long value(Long cursor) { return cursor == null ? 0L : cursor; }
}
